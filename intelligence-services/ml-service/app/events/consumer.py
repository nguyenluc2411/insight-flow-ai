"""Kafka consumer for ingesting sales and inventory events."""
from __future__ import annotations

import logging
import threading
from datetime import datetime, timezone
from uuid import UUID

from confluent_kafka import Consumer, KafkaError, KafkaException
from pydantic import ValidationError

from app.config import settings
from app.db.database import SessionLocal
from app.db.models import InventorySnapshot, SalesData
from app.events.schemas import InventoryUpdatedEvent, OrderCompletedEvent

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

    def _handle_message(self, topic: str, value: bytes) -> None:
        try:
            raw = value.decode("utf-8")
        except UnicodeDecodeError:
            logger.warning("Failed to decode message bytes on topic=%s", topic, exc_info=True)
            return

        if topic == TOPIC_SALES_COMPLETED:
            self._handle_sales_completed(raw)
        elif topic == TOPIC_INVENTORY_UPDATED:
            self._handle_inventory_updated(raw)

    def _handle_sales_completed(self, raw: str) -> None:
        try:
            event = OrderCompletedEvent.model_validate_json(raw)
        except ValidationError:
            logger.warning("Failed to parse OrderCompletedEvent", exc_info=True)
            return

        session = SessionLocal()
        try:
            occurred_at = event.occurred_at.replace(tzinfo=timezone.utc) \
                if event.occurred_at.tzinfo is None else event.occurred_at

            for item in event.items:
                exists = session.query(SalesData.id).filter(
                    SalesData.event_id == UUID(event.event_id)
                ).first()
                if exists:
                    logger.debug("Skipping duplicate event_id=%s", event.event_id)
                    return
                session.add(SalesData(
                    event_id=UUID(event.event_id),
                    tenant_id=UUID(event.tenant_id),
                    variant_id=UUID(item.variant_id),
                    quantity=item.quantity,
                    unit_price=item.unit_price,
                    order_id=UUID(event.order_id) if event.order_id else None,
                    occurred_at=occurred_at,
                ))
            session.commit()
            self._check_training_readiness(event.tenant_id)
        except Exception:  # noqa: BLE001
            logger.error("DB error handling sales.order.completed", exc_info=True)
            session.rollback()
        finally:
            session.close()

    def _handle_inventory_updated(self, raw: str) -> None:
        try:
            event = InventoryUpdatedEvent.model_validate_json(raw)
        except ValidationError:
            logger.warning("Failed to parse InventoryUpdatedEvent", exc_info=True)
            return

        session = SessionLocal()
        try:
            snap = session.query(InventorySnapshot).filter(
                InventorySnapshot.tenant_id == UUID(event.tenant_id),
                InventorySnapshot.variant_id == UUID(event.variant_id),
                InventorySnapshot.location_id == UUID(event.location_id),
            ).first()
            now = datetime.now(tz=timezone.utc)
            movement_type_lower = event.movement_type.lower() if event.movement_type else ""
            if snap is None:
                snap = InventorySnapshot(
                    tenant_id=UUID(event.tenant_id),
                    variant_id=UUID(event.variant_id),
                    location_id=UUID(event.location_id),
                    quantity=event.quantity_change,
                    first_restocked_at=now if movement_type_lower == "restock" else None,
                    updated_at=now,
                )
                session.add(snap)
            else:
                snap.quantity = snap.quantity + event.quantity_change
                if movement_type_lower == "restock" and snap.first_restocked_at is None:
                    snap.first_restocked_at = now
                snap.updated_at = now
            session.commit()
            logger.info("inventory updated for variant %s", event.variant_id)
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


kafka_consumer = KafkaEventConsumer()
