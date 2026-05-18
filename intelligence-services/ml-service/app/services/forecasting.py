"""Prophet-based demand forecasting with cold-start fallback."""
from __future__ import annotations

import json
import logging
import os
import pickle
from datetime import datetime, timedelta, timezone
from pathlib import Path
from uuid import UUID

import pandas as pd
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.config import settings
from app.db.models import SalesData
from app.models.schemas import ForecastPoint

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
        )
        model.fit(data)

        version = f"v{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        path = _model_path(tenant_id, variant_id, version)
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("wb") as f:
            pickle.dump(model, f)

        metadata = {
            "version": version,
            "trained_at": datetime.now(tz=timezone.utc).isoformat(),
            "data_points": len(data),
            "variant_id": str(variant_id),
            "tenant_id": str(tenant_id),
        }
        meta_path = path.with_suffix(".json")
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
    ) -> tuple[list[ForecastPoint], str, str]:
        """Return (predictions, confidence, basis)."""
        latest = _find_latest_model(tenant_id, variant_id)
        if latest is None:
            logger.info("No model found, using category fallback for variant=%s", variant_id)
            return self._category_fallback(db, tenant_id, variant_id, days)

        try:
            with latest.open("rb") as f:
                model = pickle.load(f)
        except (OSError, pickle.UnpicklingError):
            logger.warning("Failed to load model %s, falling back", latest, exc_info=True)
            return self._category_fallback(db, tenant_id, variant_id, days)

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
    ) -> tuple[list[ForecastPoint], str, str]:
        """Moving-average fallback when no per-variant model exists."""
        logger.info("Using category fallback for variant %s", variant_id)
        cutoff = datetime.now(tz=timezone.utc) - timedelta(days=30)
        avg = (
            db.query(func.avg(SalesData.quantity))
            .filter(
                SalesData.tenant_id == tenant_id,
                SalesData.variant_id == variant_id,
                SalesData.occurred_at >= cutoff,
            )
            .scalar()
        )

        if avg is None or float(avg) <= 0:
            zeros = [
                ForecastPoint(
                    date=datetime.now(tz=timezone.utc) + timedelta(days=i + 1),
                    predicted=0.0,
                    lower_bound=0.0,
                    upper_bound=0.0,
                )
                for i in range(days)
            ]
            return zeros, "none", "moving_average"

        avg_f = float(avg)
        predictions = [
            ForecastPoint(
                date=datetime.now(tz=timezone.utc) + timedelta(days=i + 1),
                predicted=avg_f,
                lower_bound=max(0.0, avg_f * 0.5),
                upper_bound=avg_f * 1.5,
            )
            for i in range(days)
        ]
        return predictions, "low", "moving_average"

    def count_loaded_models(self) -> int:
        """Count model files on disk (rough proxy for 'models loaded')."""
        root = Path(settings.MODEL_STORAGE_PATH)
        if not root.exists():
            return 0
        return sum(1 for _ in root.rglob("*.pkl"))


def _model_path(tenant_id: UUID, variant_id: UUID, version: str) -> Path:
    return (
        Path(settings.MODEL_STORAGE_PATH)
        / str(tenant_id)
        / str(variant_id)
        / f"{version}.pkl"
    )


def _find_latest_model(tenant_id: UUID, variant_id: UUID) -> Path | None:
    base = Path(settings.MODEL_STORAGE_PATH) / str(tenant_id) / str(variant_id)
    if not base.exists():
        return None
    candidates = sorted(base.glob("*.pkl"), key=os.path.getmtime, reverse=True)
    return candidates[0] if candidates else None


forecaster = ProphetForecaster()
