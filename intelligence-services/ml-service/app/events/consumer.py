"""Kafka consumer for ingesting sales and inventory events."""
from __future__ import annotations

import logging
import threading
from datetime import datetime, timezone
from pathlib import Path
from uuid import UUID

from confluent_kafka import Consumer, KafkaError, KafkaException
from pydantic import ValidationError

from app.config import settings
from app.db.database import SessionLocal
from app.db.models import InventorySnapshot, SalesData, VariantCategoryMap
from app.events.schemas import (
    CatalogOrderNormalizedEvent,
    InventoryUpdatedEvent,
    OrderCompletedEvent,
)
from app.utils.category_mapper import resolve_category_key

logger = logging.getLogger(__name__)

TOPIC_SALES_COMPLETED = "sales.order.completed"
TOPIC_INVENTORY_UPDATED = "catalog.inventory.updated"
TOPIC_ORDER_NORMALIZED = "catalog.order.normalized"


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
            self._consumer.subscribe([
                TOPIC_SALES_COMPLETED, TOPIC_INVENTORY_UPDATED, TOPIC_ORDER_NORMALIZED,
            ])
            self._connected = True
            logger.info("Subscribed to topics: %s, %s, %s",
                        TOPIC_SALES_COMPLETED, TOPIC_INVENTORY_UPDATED, TOPIC_ORDER_NORMALIZED)
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
        elif topic == TOPIC_ORDER_NORMALIZED:
            self._handle_order_normalized(raw)

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

            saved = 0
            for item in event.items:
                # Dedup per (event_id, variant_id) — one order can have multiple variants
                exists = session.query(SalesData.id).filter(
                    SalesData.event_id == UUID(event.event_id),
                    SalesData.variant_id == UUID(item.variant_id),
                ).first()
                if exists:
                    logger.debug("Skipping duplicate event_id=%s variant=%s", event.event_id, item.variant_id)
                    continue
                session.add(SalesData(
                    event_id=UUID(event.event_id),
                    tenant_id=UUID(event.tenant_id),
                    variant_id=UUID(item.variant_id),
                    quantity=item.quantity,
                    unit_price=item.unit_price,
                    order_id=UUID(event.order_id) if event.order_id else None,
                    occurred_at=occurred_at,
                ))
                saved += 1
            if saved > 0:
                session.commit()
                self._check_training_readiness(event.tenant_id)
        except Exception:  # noqa: BLE001
            logger.error("DB error handling sales.order.completed", exc_info=True)
            session.rollback()
        finally:
            session.close()

    def _handle_order_normalized(self, raw: str) -> None:
        """POS orders normalized by catalog (variant-keyed) — backfill training data."""
        try:
            event = CatalogOrderNormalizedEvent.model_validate_json(raw)
        except ValidationError:
            logger.warning("Failed to parse CatalogOrderNormalizedEvent", exc_info=True)
            return

        # Use the POS purchase time as the ML time axis; fall back to emission time.
        ts = event.ordered_at or event.occurred_at
        occurred_at = ts.replace(tzinfo=timezone.utc) if ts.tzinfo is None else ts

        session = SessionLocal()
        try:
            saved = 0
            for item in event.items:
                exists = session.query(SalesData.id).filter(
                    SalesData.event_id == UUID(event.event_id),
                    SalesData.variant_id == UUID(item.variant_id),
                ).first()
                if exists:
                    continue
                session.add(SalesData(
                    event_id=UUID(event.event_id),
                    tenant_id=UUID(event.tenant_id),
                    variant_id=UUID(item.variant_id),
                    quantity=item.quantity,
                    unit_price=item.unit_price,
                    order_id=None,  # external POS order id is not an internal UUID
                    occurred_at=occurred_at,
                ))
                saved += 1
            if saved > 0:
                session.commit()
                self._check_training_readiness(event.tenant_id)
        except Exception:  # noqa: BLE001
            logger.error("DB error handling catalog.order.normalized", exc_info=True)
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

            # Upsert variant → category_key mapping for cold-start forecasting.
            if event.category_slug or event.category_name:
                mapped = resolve_category_key(event.category_slug, event.category_name)
                mapping = session.get(VariantCategoryMap, UUID(event.variant_id))
                if mapping is None:
                    session.add(VariantCategoryMap(
                        variant_id=UUID(event.variant_id),
                        tenant_id=UUID(event.tenant_id),
                        category_key=mapped,
                    ))
                else:
                    mapping.category_key = mapped

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
            if count < settings.MIN_DATA_POINTS:
                return

            # Auto-trigger first-time training when no model exists yet for this tenant
            tenant_model_dir = Path(settings.MODEL_STORAGE_PATH) / tenant_id
            has_model = tenant_model_dir.exists() and any(tenant_model_dir.rglob("*.pkl"))
            if not has_model:
                logger.info(
                    "Tenant %s crossed threshold (%d points) — auto-triggering first training",
                    tenant_id, count,
                )
                # Late import avoids circular dependency at module load time
                from app.api.training import start_training_background  # noqa: PLC0415
                start_training_background(UUID(tenant_id))
            else:
                logger.debug("Tenant %s has %d data points and existing models", tenant_id, count)
        except Exception:  # noqa: BLE001
            logger.warning("Could not check training readiness", exc_info=True)
        finally:
            session.close()


kafka_consumer = KafkaEventConsumer()
