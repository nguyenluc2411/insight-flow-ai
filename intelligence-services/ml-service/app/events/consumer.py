"""Kafka consumer for ingesting sales and inventory events."""
from __future__ import annotations

import json
import logging
import threading
from datetime import datetime, timezone
from uuid import UUID

from confluent_kafka import Consumer, KafkaError, KafkaException

from app.config import settings
from app.db.database import SessionLocal
from app.db.models import InventorySnapshot, SalesData

logger = logging.getLogger(__name__)

TOPIC_SALES_COMPLETED = "sales.order.completed"
TOPIC_INVENTORY_UPDATED = "catalog.inventory.updated"


class KafkaEventConsumer:
    """Background Kafka consumer; survives transient parse/db errors."""

    def __init__(self) -> None:
        self._consumer: Consumer | None = None
        self._thread: threading.Thread | None = None
        self._running = False
        self._connected = False

    @property
    def is_connected(self) -> bool:
        return self._connected

    def start(self) -> None:
        if self._running:
            return
        self._running = True
        self._thread = threading.Thread(target=self._run, name="kafka-consumer", daemon=True)
        self._thread.start()
        logger.info("Kafka consumer thread started")

    def stop(self) -> None:
        self._running = False
        if self._consumer is not None:
            try:
                self._consumer.close()
            except Exception:  # noqa: BLE001
                logger.warning("Error closing Kafka consumer", exc_info=True)
        if self._thread is not None:
            self._thread.join(timeout=5)
        logger.info("Kafka consumer stopped")

    def _run(self) -> None:
        try:
            self._consumer = Consumer({
                "bootstrap.servers": settings.KAFKA_BOOTSTRAP,
                "group.id": settings.KAFKA_GROUP_ID,
                "auto.offset.reset": "earliest",
                "enable.auto.commit": True,
            })
            self._consumer.subscribe([TOPIC_SALES_COMPLETED, TOPIC_INVENTORY_UPDATED])
            self._connected = True
            logger.info("Subscribed to topics: %s, %s", TOPIC_SALES_COMPLETED, TOPIC_INVENTORY_UPDATED)
        except KafkaException:
            logger.error("Failed to initialize Kafka consumer", exc_info=True)
            self._connected = False
            return

        while self._running:
            try:
                msg = self._consumer.poll(timeout=1.0)
                if msg is None:
                    continue
                if msg.error():
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        continue
                    logger.warning("Kafka message error: %s", msg.error())
                    continue
                self._handle_message(msg.topic(), msg.value())
            except Exception:  # noqa: BLE001
                logger.error("Unexpected error in consumer loop", exc_info=True)
                # never crash the loop

    def _handle_message(self, topic: str, value: bytes) -> None:
        try:
            payload = json.loads(value.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError):
            logger.warning("Failed to parse JSON payload on topic=%s", topic, exc_info=True)
            return

        if topic == TOPIC_SALES_COMPLETED:
            self._handle_sales_completed(payload)
        elif topic == TOPIC_INVENTORY_UPDATED:
            self._handle_inventory_updated(payload)

    def _handle_sales_completed(self, payload: dict) -> None:
        event_id = payload.get("eventId")
        tenant_id = payload.get("tenantId")
        items = payload.get("items", [])
        if not event_id or not tenant_id:
            logger.warning("sales.order.completed missing eventId or tenantId")
            return

        occurred_raw = payload.get("occurredAt")
        occurred_at = _parse_event_time(occurred_raw) or datetime.now(tz=timezone.utc)

        session = SessionLocal()
        try:
            for item in items:
                variant_id = item.get("variantId")
                quantity = item.get("quantity")
                if not variant_id or quantity is None:
                    continue
                # idempotency: skip if any row already exists for this event_id
                exists = session.query(SalesData.id).filter(
                    SalesData.event_id == UUID(event_id)
                ).first()
                if exists:
                    logger.debug("Skipping duplicate event_id=%s", event_id)
                    return
                session.add(SalesData(
                    event_id=UUID(event_id),
                    tenant_id=UUID(tenant_id),
                    variant_id=UUID(variant_id),
                    quantity=int(quantity),
                    unit_price=item.get("unitPrice"),
                    order_id=UUID(payload["orderId"]) if payload.get("orderId") else None,
                    occurred_at=occurred_at,
                ))
            session.commit()
            self._check_training_readiness(tenant_id)
        except Exception:  # noqa: BLE001
            logger.error("DB error handling sales.order.completed", exc_info=True)
            session.rollback()
        finally:
            session.close()

    def _handle_inventory_updated(self, payload: dict) -> None:
        tenant_id = payload.get("tenantId")
        variant_id = payload.get("variantId")
        location_id = payload.get("locationId")
        if not (tenant_id and variant_id and location_id):
            logger.warning("catalog.inventory.updated missing required field")
            return

        quantity_change = payload.get("quantityChange", 0)
        movement_type = payload.get("movementType")

        session = SessionLocal()
        try:
            snap = session.query(InventorySnapshot).filter(
                InventorySnapshot.tenant_id == UUID(tenant_id),
                InventorySnapshot.variant_id == UUID(variant_id),
                InventorySnapshot.location_id == UUID(location_id),
            ).first()
            now = datetime.now(tz=timezone.utc)
            if snap is None:
                snap = InventorySnapshot(
                    tenant_id=UUID(tenant_id),
                    variant_id=UUID(variant_id),
                    location_id=UUID(location_id),
                    quantity=int(quantity_change),
                    first_restocked_at=now if movement_type == "restock" else None,
                    updated_at=now,
                )
                session.add(snap)
            else:
                snap.quantity = snap.quantity + int(quantity_change)
                if movement_type == "restock" and snap.first_restocked_at is None:
                    snap.first_restocked_at = now
                snap.updated_at = now
            session.commit()
            logger.info("inventory updated for variant %s", variant_id)
        except Exception:  # noqa: BLE001
            logger.error("DB error handling catalog.inventory.updated", exc_info=True)
            session.rollback()
        finally:
            session.close()

    def _check_training_readiness(self, tenant_id: str) -> None:
        session = SessionLocal()
        try:
            count = session.query(SalesData).filter(
                SalesData.tenant_id == UUID(tenant_id)
            ).count()
            if count >= settings.MIN_DATA_POINTS:
                logger.info("Tenant %s ready to train (%d data points)", tenant_id, count)
        except Exception:  # noqa: BLE001
            logger.warning("Could not check training readiness", exc_info=True)
        finally:
            session.close()


def _parse_event_time(raw) -> datetime | None:
    if raw is None:
        return None
    try:
        if isinstance(raw, (int, float)):
            return datetime.fromtimestamp(float(raw), tz=timezone.utc)
        return datetime.fromisoformat(str(raw).replace("Z", "+00:00"))
    except (ValueError, TypeError):
        return None


kafka_consumer = KafkaEventConsumer()
