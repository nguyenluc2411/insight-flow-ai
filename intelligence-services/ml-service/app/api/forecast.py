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


@router.get("/{variant_id}", response_model=ForecastResponse)
def get_forecast(
    variant_id: UUID,
    x_tenant_id: UUID = Header(..., alias="X-Tenant-Id"),
    days: int = Query(default=30, ge=1, le=90),
    db: Session = Depends(get_db),
) -> ForecastResponse:
    predictions, confidence, basis = forecaster.predict(
        db, x_tenant_id, variant_id, days
    )
    return ForecastResponse(
        variant_id=variant_id,
        tenant_id=x_tenant_id,
        forecast_days=days,
        confidence=confidence,
        basis=basis,
        predictions=predictions,
        generated_at=datetime.now(tz=timezone.utc),
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
        preds, confidence, basis = forecaster.predict(
            db, x_tenant_id, variant_id, request.days
        )
        out.append(
            ForecastResponse(
                variant_id=variant_id,
                tenant_id=x_tenant_id,
                forecast_days=request.days,
                confidence=confidence,
                basis=basis,
                predictions=preds,
                generated_at=now,
            )
        )
    return out
