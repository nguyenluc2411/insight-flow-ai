"""SQLAlchemy ORM models for ml_service_db schema."""

from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import (
    BigInteger,
    Column,
    DateTime,
    Float,
    Integer,
    Numeric,
    String,
    Text,
    UniqueConstraint,
    func,
)
from sqlalchemy.dialects.postgresql import UUID

from app.config import settings
from app.db.database import Base

SCHEMA = settings.DB_SCHEMA


class Forecast(Base):
    __tablename__ = "forecasts"
    __table_args__ = (
        UniqueConstraint(
            "tenant_id",
            "variant_id",
            "forecast_date",
            name="uq_forecast_tenant_variant_date",
        ),
        {"schema": SCHEMA},
    )

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    tenant_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    variant_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    forecast_date = Column(DateTime(timezone=True), nullable=False)
    predicted_quantity = Column(Float, nullable=False)
    lower_bound = Column(Float, nullable=True)
    upper_bound = Column(Float, nullable=True)
    confidence_level = Column(Float, nullable=False, default=0.8)
    model_version = Column(String(50), nullable=True)
    created_at = Column(
        DateTime(timezone=True), nullable=False, default=datetime.utcnow
    )


class Recommendation(Base):
    __tablename__ = "recommendations"
    __table_args__ = ({"schema": SCHEMA},)

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    tenant_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    variant_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    action = Column(String(20), nullable=False)
    reason = Column(Text, nullable=True)
    priority = Column(String(10), nullable=False)
    suggested_discount_pct = Column(Float, nullable=True)
    suggested_restock_qty = Column(Integer, nullable=True)
    stock_age_days = Column(Integer, nullable=True)
    current_stock = Column(Integer, nullable=True)
    sales_velocity_30d = Column(Float, nullable=True)
    created_at = Column(
        DateTime(timezone=True), nullable=False, default=datetime.utcnow
    )


class TrainingJob(Base):
    __tablename__ = "training_jobs"
    __table_args__ = ({"schema": SCHEMA},)

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    tenant_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    status = Column(String(20), nullable=False, default="PENDING")
    variants_trained = Column(Integer, nullable=True, default=0)
    error_message = Column(Text, nullable=True)
    started_at = Column(DateTime(timezone=True), nullable=True)
    completed_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(
        DateTime(timezone=True), nullable=False, default=datetime.utcnow
    )


class SalesData(Base):
    """Local sales data accumulated from Kafka consumer for forecast training."""

    __tablename__ = "sales_data"
    __table_args__ = (
        # One row per (event, variant) — an order can contain multiple variant items.
        # unique=True on event_id alone would block saving items 2..N of the same order.
        UniqueConstraint("event_id", "variant_id", name="uq_sales_data_event_variant"),
        {"schema": SCHEMA},
    )

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    event_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    tenant_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    variant_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    quantity = Column(Integer, nullable=False)
    unit_price = Column(Numeric(15, 2), nullable=True)
    order_id = Column(UUID(as_uuid=True), nullable=True)
    occurred_at = Column(DateTime(timezone=True), nullable=False)
    created_at = Column(
        DateTime(timezone=True), nullable=False, default=datetime.utcnow
    )


class VariantCategoryMap(Base):
    """Map variant_id → category_key for cold-start forecasting.

    Populated from the catalog.inventory.updated Kafka event (raw category slug/
    name mapped via app.utils.category_mapper). No cross-service DB join: catalog
    owns the data and ships it on the event; ml owns the base-model taxonomy.
    """
    __tablename__ = "variant_category_map"
    __table_args__ = ({"schema": SCHEMA},)

    variant_id = Column(UUID(as_uuid=True), primary_key=True)
    tenant_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    # One of the 14 base-model keys, or "unknown" when nothing maps.
    category_key = Column(String(50), nullable=False)
    updated_at = Column(DateTime(timezone=True), nullable=False, default=func.now(), onupdate=func.now())


class InventorySnapshot(Base):
    """Latest inventory state per (tenant, variant, location) from catalog events."""

    __tablename__ = "inventory_snapshots"
    __table_args__ = (
        UniqueConstraint(
            "tenant_id", "variant_id", "location_id", name="uq_inv_snapshot_tvl"
        ),
        {"schema": SCHEMA},
    )

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    tenant_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    variant_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    location_id = Column(UUID(as_uuid=True), nullable=False)
    quantity = Column(Integer, nullable=False, default=0)
    first_restocked_at = Column(DateTime(timezone=True), nullable=True)
    updated_at = Column(
        DateTime(timezone=True), nullable=False, default=datetime.utcnow
    )


class InventoryAdvice(Base):
    """LLM-generated inventory strategy for a file-upload workspace.

    Folded in from the former Java recommendation-service: one row per workspace
    ingestion run, holding the raw Gemini JSON (strategy + trend forecasting).
    """

    __tablename__ = "inventory_advice"
    __table_args__ = (
        UniqueConstraint("workspace_id", name="uq_inventory_advice_workspace"),
        {"schema": SCHEMA},
    )

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    tenant_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    workspace_id = Column(String(36), nullable=False, index=True)
    status = Column(String(20), nullable=False)  # PROCESSING, DONE, ERROR
    result_json = Column(Text, nullable=True)
    error_log = Column(Text, nullable=True)
    created_at = Column(
        DateTime(timezone=True), nullable=False, default=datetime.utcnow
    )
    updated_at = Column(
        DateTime(timezone=True), nullable=False, default=datetime.utcnow
    )
