"""Recommendation endpoints."""
from __future__ import annotations

import logging
import threading
from datetime import datetime, timezone
from typing import Literal
from uuid import UUID

from fastapi import APIRouter, Depends, Header, Query
from sqlalchemy.orm import Session

from app.db.database import SessionLocal, get_db
from app.db.models import Recommendation, TrainingJob
from app.models.schemas import (
    PagedRecommendationResponse,
    RecommendationResponse,
    RefreshJobResponse,
)
from app.services.recommendation import recommender

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/ml/recommendations", tags=["Recommendation"])


@router.get("", response_model=PagedRecommendationResponse)
def list_recommendations(
    x_tenant_id: UUID = Header(..., alias="X-Tenant-Id"),
    action: Literal["CLEARANCE", "RESTOCK", "PROMOTE", "OK"] | None = Query(default=None),
    priority: Literal["HIGH", "MEDIUM", "LOW"] | None = Query(default=None),
    page: int = Query(default=0, ge=0),
    size: int = Query(default=20, ge=1, le=100),
    db: Session = Depends(get_db),
) -> PagedRecommendationResponse:
    query = db.query(Recommendation).filter(Recommendation.tenant_id == x_tenant_id)
    if action:
        query = query.filter(Recommendation.action == action)
    if priority:
        query = query.filter(Recommendation.priority == priority)

    total = query.count()
    rows = (
        query.order_by(Recommendation.created_at.desc())
        .offset(page * size)
        .limit(size)
        .all()
    )
    items = [RecommendationResponse.model_validate(r) for r in rows]
    return PagedRecommendationResponse(items=items, page=page, size=size, total=total)


@router.post("/refresh", response_model=RefreshJobResponse, status_code=202)
def refresh_recommendations(
    x_tenant_id: UUID = Header(..., alias="X-Tenant-Id"),
    db: Session = Depends(get_db),
) -> RefreshJobResponse:
    job = TrainingJob(tenant_id=x_tenant_id, status="PENDING")
    db.add(job)
    db.commit()
    db.refresh(job)

    threading.Thread(
        target=_run_refresh_job,
        args=(job.id, x_tenant_id),
        daemon=True,
        name=f"refresh-{job.id}",
    ).start()

    return RefreshJobResponse(job_id=job.id, status="PENDING")


def _run_refresh_job(job_id: UUID, tenant_id: UUID) -> None:
    session = SessionLocal()
    try:
        job = session.query(TrainingJob).filter(TrainingJob.id == job_id).first()
        if job is None:
            return
        job.status = "RUNNING"
        job.started_at = datetime.now(tz=timezone.utc)
        session.commit()

        results = recommender.generate_for_tenant(session, tenant_id)

        job.status = "SUCCESS"
        job.variants_trained = len(results)
        job.completed_at = datetime.now(tz=timezone.utc)
        session.commit()
    except Exception as exc:  # noqa: BLE001
        logger.error("Refresh job failed", exc_info=True)
        try:
            job = session.query(TrainingJob).filter(TrainingJob.id == job_id).first()
            if job is not None:
                job.status = "FAILED"
                job.error_message = str(exc)
                job.completed_at = datetime.now(tz=timezone.utc)
                session.commit()
        except Exception:  # noqa: BLE001
            logger.error("Could not update failed job status", exc_info=True)
    finally:
        session.close()
