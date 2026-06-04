"""Minimal Kafka producer for emitting inventory-advice lifecycle events.

Emits the same nested {envelope, payload} shape the file-upload services expect
(user-workspace reads payload.workspace_id to flip the workspace status).
"""

from __future__ import annotations

import json
import logging
import uuid
from datetime import datetime, timezone

from confluent_kafka import Producer

from app.config import settings

logger = logging.getLogger(__name__)

TOPIC_RECOMMENDATION_GENERATED = "inventory.recommendation.generated"
TOPIC_RECOMMENDATION_FAILED = "inventory.recommendation.failed"

_producer: Producer | None = None


def _get_producer() -> Producer:
    global _producer
    if _producer is None:
        _producer = Producer({"bootstrap.servers": settings.KAFKA_BOOTSTRAP})
    return _producer


def _envelope(event_type: str, payload: dict) -> str:
    return json.dumps(
        {
            "event_id": str(uuid.uuid4()),
            "event_type": event_type,
            "timestamp": datetime.now(tz=timezone.utc).isoformat(),
            "source": "ml-service",
            "payload": payload,
        }
    )


def send_recommendation_generated(workspace_id: str, tenant_id: str) -> None:
    payload = {"workspace_id": workspace_id, "tenant_id": tenant_id}
    _emit(TOPIC_RECOMMENDATION_GENERATED, workspace_id, _envelope(TOPIC_RECOMMENDATION_GENERATED, payload))


def send_recommendation_failed(workspace_id: str, error_message: str) -> None:
    payload = {"workspace_id": workspace_id, "error_message": error_message}
    _emit(TOPIC_RECOMMENDATION_FAILED, workspace_id, _envelope(TOPIC_RECOMMENDATION_FAILED, payload))


def _emit(topic: str, key: str, value: str) -> None:
    try:
        producer = _get_producer()
        producer.produce(topic, key=key, value=value)
        producer.flush(5)
        logger.info("Emitted %s for workspace %s", topic, key)
    except Exception:  # noqa: BLE001 — emitting must not crash the advisor flow
        logger.error("Failed to emit %s for workspace %s", topic, key, exc_info=True)
