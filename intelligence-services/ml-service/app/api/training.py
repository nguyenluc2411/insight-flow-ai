"""Training endpoints and background job runner."""
from __future__ import annotations

import logging
import threading
from datetime import datetime, timezone
from uuid import UUID

from fastapi import APIRouter, Depends, Header, HTTPException
from sqlalchemy.orm import Session

from app.config import settings
from app.db.database import SessionLocal, get_db
from app.db.models import SalesData, TrainingJob
from app.models.schemas import TrainJobResponse
from app.services.forecasting import InsufficientDataError, forecaster

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/ml", tags=["Training"])


@router.post("/train", response_model=TrainJobResponse, status_code=202)
def trigger_training(
    x_tenant_id: UUID = Header(..., alias="X-Tenant-Id"),
    db: Session = Depends(get_db),
) -> TrainJobResponse:
    """Trigger Prophet model training for all variants of a tenant.

    Requires at least MIN_DATA_POINTS sales records accumulated from Kafka.
    Returns 202 immediately; poll GET /train/{job_id} for result.
    """
    count = db.query(SalesData).filter(SalesData.tenant_id == x_tenant_id).count()
    if count < settings.MIN_DATA_POINTS:
        raise HTTPException(
            status_code=422,
            detail=(
                f"Cần tối thiểu {settings.MIN_DATA_POINTS} data points để train, "
                f"hiện có {count}. Hãy đợi thêm đơn hàng được sync qua Kafka."
            ),
        )

    job = TrainingJob(tenant_id=x_tenant_id, status="PENDING")
    db.add(job)
    db.commit()
    db.refresh(job)

    threading.Thread(
        target=_run_training_job,
        args=(job.id, x_tenant_id),
        daemon=True,
        name=f"train-{job.id}",
    ).start()

    return TrainJobResponse(
        job_id=job.id,
        status="PENDING",
        data_points=count,
    )


@router.get("/train/{job_id}", response_model=TrainJobResponse)
def get_training_job(
    job_id: UUID,
    x_tenant_id: UUID = Header(..., alias="X-Tenant-Id"),
    db: Session = Depends(get_db),
) -> TrainJobResponse:
    """Poll training job status."""
    job = (
        db.query(TrainingJob)
        .filter(TrainingJob.id == job_id, TrainingJob.tenant_id == x_tenant_id)
        .first()
    )
    if job is None:
        raise HTTPException(status_code=404, detail="Training job not found")
    return TrainJobResponse.model_validate(job)


def start_training_background(tenant_id: UUID) -> UUID:
    """Spawn a background training thread and return the job_id.

    Called by the Kafka consumer for auto first-time training.
    """
    session = SessionLocal()
    try:
        job = TrainingJob(tenant_id=tenant_id, status="PENDING")
        session.add(job)
        session.commit()
        session.refresh(job)
        job_id = job.id
    finally:
        session.close()

    threading.Thread(
        target=_run_training_job,
        args=(job_id, tenant_id),
        daemon=True,
        name=f"train-{job_id}",
    ).start()
    logger.info("Background training job %s started for tenant %s", job_id, tenant_id)
    return job_id


def _run_training_job(job_id: UUID, tenant_id: UUID) -> None:
    session = SessionLocal()
    try:
        job = session.query(TrainingJob).filter(TrainingJob.id == job_id).first()
        if job is None:
            return

        job.status = "RUNNING"
        job.started_at = datetime.now(tz=timezone.utc)
        session.commit()

        variant_ids = [
            v[0]
            for v in session.query(SalesData.variant_id)
            .filter(SalesData.tenant_id == tenant_id)
            .distinct()
            .all()
        ]

        trained = 0
        errors: list[str] = []
        for variant_id in variant_ids:
            try:
                forecaster.train(session, tenant_id, variant_id)
                trained += 1
            except InsufficientDataError:
                pass  # variant has < MIN_DATA_POINTS — skip silently
            except Exception as exc:  # noqa: BLE001
                logger.warning("Failed to train variant %s: %s", variant_id, exc)
                errors.append(f"{variant_id}: {exc}")

        job.status = "SUCCESS"
        job.variants_trained = trained
        job.error_message = "; ".join(errors[:5]) if errors else None
        job.completed_at = datetime.now(tz=timezone.utc)
        session.commit()
        logger.info(
            "Training job %s completed: %d variants trained, %d errors",
            job_id, trained, len(errors),
        )
    except Exception as exc:  # noqa: BLE001
        logger.error("Training job %s failed", job_id, exc_info=True)
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
