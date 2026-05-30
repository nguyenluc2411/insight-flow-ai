"""Prophet-based demand forecasting with cold-start fallback."""
from __future__ import annotations

import json
import logging
from datetime import datetime, timedelta, timezone
from pathlib import Path
from uuid import UUID

import pandas as pd
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.config import settings
from app.db.models import SalesData, VariantCategoryMap
from app.models.schemas import ForecastPoint
from app.models.storage import list_keys, load_model, model_exists, save_model
from app.utils.vn_holidays import VN_HOLIDAYS

logger = logging.getLogger(__name__)


class InsufficientDataError(Exception):
    """Raised when there is not enough history to train a variant-level model."""


class ProphetForecaster:
    """Per-tenant per-variant Prophet forecaster with category fallback."""

    def prepare_data(self, db: Session, tenant_id: UUID, variant_id: UUID) -> pd.DataFrame:
        """Aggregate sales by day; returns DataFrame with columns ds, y."""
        rows = (
            db.query(
                func.date_trunc("day", SalesData.occurred_at).label("ds"),
                func.sum(SalesData.quantity).label("y"),
            )
            .filter(
                SalesData.tenant_id == tenant_id,
                SalesData.variant_id == variant_id,
            )
            .group_by("ds")
            .order_by("ds")
            .all()
        )
        if not rows:
            return pd.DataFrame(columns=["ds", "y"])
        df = pd.DataFrame(rows, columns=["ds", "y"])
        df["ds"] = pd.to_datetime(df["ds"]).dt.tz_localize(None)
        df["y"] = df["y"].astype(float)
        return df

    def train(self, db: Session, tenant_id: UUID, variant_id: UUID) -> str:
        """Train a variant-level Prophet model and persist it to disk."""
        from prophet import Prophet  # local import keeps cold-start light

        data = self.prepare_data(db, tenant_id, variant_id)
        if len(data) < settings.MIN_DATA_POINTS:
            raise InsufficientDataError(
                f"Cần {settings.MIN_DATA_POINTS} data points, hiện có {len(data)}"
            )

        model = Prophet(
            yearly_seasonality=True,
            weekly_seasonality=True,
            daily_seasonality=False,
            seasonality_mode="multiplicative",
            # Holiday effects via dummy variables — Taylor & Letham (2018).
            # Captures Tết / 8-3 / 20-10 / 11-11 / Black Friday / Noel spikes.
            holidays=VN_HOLIDAYS,
        )
        model.fit(data)

        version = f"v{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        key = _variant_key(tenant_id, variant_id, version)
        save_model(model, key)

        metadata = {
            "version": version,
            "trained_at": datetime.now(tz=timezone.utc).isoformat(),
            "data_points": len(data),
            "variant_id": str(variant_id),
            "tenant_id": str(tenant_id),
        }
        meta_path = (Path(settings.MODEL_STORAGE_PATH) / key).with_suffix(".json")
        meta_path.parent.mkdir(parents=True, exist_ok=True)
        with meta_path.open("w", encoding="utf-8") as f:
            json.dump(metadata, f)

        logger.info("Trained model %s for tenant=%s variant=%s", version, tenant_id, variant_id)
        return version

    def predict(
        self,
        db: Session,
        tenant_id: UUID,
        variant_id: UUID,
        days: int = 30,
        category_key: str | None = None,
    ) -> tuple[list[ForecastPoint], str, str]:
        """Return (predictions, confidence, basis).

        Args:
            category_key: Optional fashion category for cold-start. When the shop
                has no per-variant model, the HCM market base model for this
                category is used instead of returning zeros.
        """
        latest_key = _find_latest_model_key(tenant_id, variant_id)
        if latest_key is None:
            logger.info("No model found, using category fallback for variant=%s", variant_id)
            return self._category_fallback(db, tenant_id, variant_id, days, category_key)

        try:
            model = load_model(latest_key)
        except (FileNotFoundError, OSError):
            logger.warning("Failed to load model %s, falling back", latest_key, exc_info=True)
            return self._category_fallback(db, tenant_id, variant_id, days, category_key)

        future = model.make_future_dataframe(periods=days)
        forecast_df = model.predict(future)
        tail = forecast_df.tail(days)

        predictions = [
            ForecastPoint(
                date=row.ds.to_pydatetime().replace(tzinfo=timezone.utc),
                predicted=float(row.yhat),
                lower_bound=float(row.yhat_lower),
                upper_bound=float(row.yhat_upper),
            )
            for row in tail.itertuples(index=False)
        ]
        return predictions, "high", "variant"

    def _category_fallback(
        self,
        db: Session,
        tenant_id: UUID,
        variant_id: UUID,
        days: int,
        category_key: str | None = None,
    ) -> tuple[list[ForecastPoint], str, str]:
        """Cold-start: forecast from the HCM market base model for the category.

        A brand-new shop has no per-variant history, so we use the pre-trained
        category base model (Google Trends, geo=VN-SG — see
        scripts/build_base_model.py) as the prior. Returns zeros only when the
        category is unknown or no base model has been built yet.

        Base-model predictions reflect the seasonal SHAPE (relative Google Trends
        index, 0–100), NOT absolute units; the level is calibrated later once the
        shop accumulates real POS sales.
        """
        if category_key is None:
            category_key = _resolve_category_key(db, tenant_id, variant_id)
        if category_key is None:
            logger.info("Cold-start with unknown category for variant=%s — no base model", variant_id)
            return self._zeros(days), "none", "no_base_model"

        base_key = _base_key(category_key)
        if not model_exists(base_key):
            logger.warning("No base model for category=%s (variant=%s)", category_key, variant_id)
            return self._zeros(days), "none", "no_base_model"

        try:
            model = load_model(base_key)
        except (FileNotFoundError, OSError):
            logger.warning("Failed to load base model %s, returning zeros", base_key, exc_info=True)
            return self._zeros(days), "none", "no_base_model"

        future = model.make_future_dataframe(periods=days, freq="D")
        forecast_df = model.predict(future)
        tail = forecast_df.tail(days)

        predictions = [
            ForecastPoint(
                date=row.ds.to_pydatetime().replace(tzinfo=timezone.utc),
                predicted=max(0.0, float(row.yhat)),
                lower_bound=max(0.0, float(row.yhat_lower)),
                upper_bound=max(0.0, float(row.yhat_upper)),
            )
            for row in tail.itertuples(index=False)
        ]
        logger.info("Cold-start forecast from base model category=%s for variant=%s", category_key, variant_id)
        return predictions, "low", "market_trends_hcm"

    def _zeros(self, days: int) -> list[ForecastPoint]:
        """Empty forecast used when no model or base model is available."""
        now = datetime.now(tz=timezone.utc)
        return [
            ForecastPoint(
                date=now + timedelta(days=i + 1),
                predicted=0.0,
                lower_bound=0.0,
                upper_bound=0.0,
            )
            for i in range(days)
        ]

    def count_loaded_models(self) -> int:
        """Count model files on disk (rough proxy for 'models loaded')."""
        root = Path(settings.MODEL_STORAGE_PATH)
        if not root.exists():
            return 0
        return sum(1 for _ in root.rglob("*.pkl"))


def _variant_key(tenant_id: UUID, variant_id: UUID, version: str) -> str:
    """Storage key (relative to MODEL_STORAGE_PATH) for a per-variant model."""
    return f"{tenant_id}/{variant_id}/{version}.pkl"


def _find_latest_model_key(tenant_id: UUID, variant_id: UUID) -> str | None:
    """Latest per-variant model key across local cache + MinIO, or None.

    Version strings are timestamp-ordered ("vYYYYMMDD_HHMMSS"), so a lexical sort
    of the keys puts the newest version last.
    """
    keys = list_keys(f"{tenant_id}/{variant_id}")
    return keys[-1] if keys else None


def _base_key(category_key: str) -> str:
    """Storage key for the pre-trained HCM market base model of a category."""
    return f"base/{category_key}.pkl"


def _resolve_category_key(db: Session, tenant_id: UUID, variant_id: UUID) -> str | None:
    """Resolve a variant to its base-model category key for cold-start.

    Reads ``variant_category_map``, populated by the catalog.inventory.updated
    consumer (raw catalog slug/name → key via app.utils.category_mapper).
    Returns None when there is no mapping yet or the mapping is "unknown", so the
    caller can pass the category explicitly via the forecast API ``category``
    parameter as a fallback.
    """
    row = (
        db.query(VariantCategoryMap.category_key)
        .filter(VariantCategoryMap.variant_id == variant_id)
        .first()
    )
    if row is None or row.category_key == "unknown":
        return None
    return row.category_key


forecaster = ProphetForecaster()
