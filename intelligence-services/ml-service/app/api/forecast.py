"""Forecast endpoints."""
from __future__ import annotations

from datetime import datetime, timezone
from uuid import UUID

from fastapi import APIRouter, Depends, Header, Query
from sqlalchemy.orm import Session

from app.db.database import get_db
from app.models.schemas import BatchForecastRequest, ForecastResponse
from app.services.forecasting import forecaster

router = APIRouter(prefix="/api/v1/ml/forecast", tags=["Forecast"])

# Shown to clients when a forecast is served from the HCM market base model
# instead of the shop's own (missing) sales history.
_COLD_START_WARNING = (
    "Dự báo dựa trên xu hướng thị trường HCM. "
    "Độ chính xác tăng dần sau 4 tuần có dữ liệu bán hàng thực tế."
)
_NO_BASE_WARNING = "Chưa có dữ liệu bán hàng và chưa có mô hình nền cho nhóm hàng này."


_GENERIC_COLD_START_WARNING = (
    "Sản phẩm chưa có danh mục. Dự báo dựa trên xu hướng thị trường thời trang HCM chung. "
    "Gán danh mục cho sản phẩm để có dự báo chính xác hơn."
)


def _cold_start_warning(basis: str) -> str | None:
    if basis == "market_trends_hcm":
        return _COLD_START_WARNING
    if basis == "market_trends_hcm_generic":
        return _GENERIC_COLD_START_WARNING
    if basis == "no_base_model":
        return _NO_BASE_WARNING
    return None


@router.get("/{variant_id}", response_model=ForecastResponse)
def get_forecast(
    variant_id: UUID,
    x_tenant_id: UUID = Header(..., alias="X-Tenant-Id"),
    days: int = Query(default=30, ge=1, le=90),
    category: str | None = Query(
        default=None,
        description="Fashion category key for cold-start base model (e.g. ao_so_mi).",
    ),
    sku: str | None = Query(
        default=None,
        description="Variant SKU — used to guess category when no explicit mapping exists.",
    ),
    db: Session = Depends(get_db),
) -> ForecastResponse:
    predictions, confidence, basis = forecaster.predict(
        db, x_tenant_id, variant_id, days, category_key=category, sku=sku
    )
    return ForecastResponse(
        variant_id=variant_id,
        tenant_id=x_tenant_id,
        forecast_days=days,
        confidence=confidence,
        basis=basis,
        predictions=predictions,
        generated_at=datetime.now(tz=timezone.utc),
        warning=_cold_start_warning(basis),
    )


@router.post("/batch", response_model=list[ForecastResponse])
def get_batch_forecast(
    request: BatchForecastRequest,
    x_tenant_id: UUID = Header(..., alias="X-Tenant-Id"),
    db: Session = Depends(get_db),
) -> list[ForecastResponse]:
    out: list[ForecastResponse] = []
    now = datetime.now(tz=timezone.utc)
    for variant_id in request.variant_ids:
        preds, confidence, basis = forecaster.predict(db, x_tenant_id, variant_id, request.days)
        out.append(ForecastResponse(
            variant_id=variant_id,
            tenant_id=x_tenant_id,
            forecast_days=request.days,
            confidence=confidence,
            basis=basis,
            predictions=preds,
            generated_at=now,
            warning=_cold_start_warning(basis),
        ))
    return out
