"""FastAPI application entrypoint for ml-service."""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from pythonjsonlogger import jsonlogger
from sqlalchemy import text

from app.api import forecast, market, recommendation, training
from app.config import settings
from app.db.database import engine, init_db
from app.events.consumer import kafka_consumer
from app.models.schemas import HealthResponse
from app.services.forecasting import forecaster


def _configure_logging() -> None:
    handler = logging.StreamHandler()
    fmt = jsonlogger.JsonFormatter("%(asctime)s %(levelname)s %(name)s %(message)s")
    handler.setFormatter(fmt)
    root = logging.getLogger()
    root.handlers.clear()
    root.addHandler(handler)
    root.setLevel(logging.INFO)


_configure_logging()
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("ml-service starting up")
    try:
        init_db()
        logger.info("Database initialized: schema=%s", settings.DB_SCHEMA)
    except Exception:  # noqa: BLE001
        logger.error("Failed to init database", exc_info=True)
    if settings.MINIO_ENABLED:
        try:
            from app.models.storage import _get_s3  # noqa: PLC0415

            s3 = _get_s3()
            existing = [b["Name"] for b in s3.list_buckets().get("Buckets", [])]
            if settings.MINIO_BUCKET not in existing:
                s3.create_bucket(Bucket=settings.MINIO_BUCKET)
                logger.info("Created MinIO bucket: %s", settings.MINIO_BUCKET)
        except Exception:  # noqa: BLE001 — never block startup on MinIO
            logger.warning(
                "MinIO bucket setup failed (model storage degraded)", exc_info=True
            )
    try:
        kafka_consumer.start()
    except Exception:  # noqa: BLE001
        logger.error("Kafka consumer failed to start", exc_info=True)
    yield
    logger.info("ml-service shutting down")
    try:
        kafka_consumer.stop()
    except Exception:  # noqa: BLE001
        logger.warning("Error stopping Kafka consumer", exc_info=True)


app = FastAPI(
    title="Insight Flow ML Service",
    description="Demand forecasting and inventory recommendation engine",
    version="1.0.0",
    lifespan=lifespan,
    # Expose OpenAPI at /v3/api-docs so the gateway Swagger aggregator
    # (which calls /v3/api-docs/{alias}) can proxy it like other services.
    openapi_url="/v3/api-docs",
    docs_url="/docs",
    redoc_url="/redoc",
)


app.include_router(forecast.router)
app.include_router(recommendation.router)
app.include_router(training.router)
app.include_router(market.router)


@app.get("/api/v1/ml/health", response_model=HealthResponse, tags=["Health"])
def health() -> HealthResponse:
    db_ok = True
    try:
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
    except Exception:  # noqa: BLE001
        db_ok = False
    return HealthResponse(
        status="UP" if db_ok else "DOWN",
        kafka_connected=kafka_consumer.is_connected,
        models_loaded=forecaster.count_loaded_models(),
        db_connected=db_ok,
    )
