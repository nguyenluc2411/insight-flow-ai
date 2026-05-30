"""Pydantic event schemas mirroring common-events Java DTOs.

Java side uses @JsonNaming(SnakeCaseStrategy) so wire format is snake_case.
Pydantic defaults to snake_case field names — no aliasing needed.
"""
from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from typing import List, Optional

from pydantic import BaseModel


class BaseEvent(BaseModel):
    event_id: str
    event_type: str
    tenant_id: str
    occurred_at: datetime


class OrderItemPayload(BaseModel):
    variant_id: str
    sku: Optional[str] = None
    quantity: int
    unit_price: Decimal
    total_price: Decimal


class OrderCompletedEvent(BaseEvent):
    order_id: str
    order_number: str
    customer_id: Optional[str] = None
    total_amount: Decimal
    currency: str
    items: List[OrderItemPayload]


class NormalizedLine(BaseModel):
    variant_id: str
    sku: Optional[str] = None
    quantity: int
    unit_price: Optional[Decimal] = None


class CatalogOrderNormalizedEvent(BaseEvent):
    connector_type: Optional[str] = None
    external_order_id: Optional[str] = None
    order_code: Optional[str] = None
    ordered_at: Optional[datetime] = None
    items: List[NormalizedLine]


class InventoryUpdatedEvent(BaseEvent):
    variant_id: str
    location_id: str
    movement_type: str
    quantity_change: int
    quantity_on_hand: int
    product_id: str
    sku: str
    reference_type: Optional[str] = None
    reference_id: Optional[str] = None
    category_name: Optional[str] = None  # raw từ catalog, vd "Áo Sơ Mi Nam"
    category_slug: Optional[str] = None  # raw từ catalog, vd "ao-so-mi-nam"


class ForecastGeneratedEvent(BaseEvent):
    variant_id: str
    forecast_horizon: int
    model_type: str
    confidence: str
    total_forecast_qty: int
    forecast_date: str


class RecommendationCreatedEvent(BaseEvent):
    variant_id: str
    action: str
    priority: str
    reason: str
    suggested_discount: Optional[int] = None
    stock_age_days: Optional[int] = None
    forecast_demand_30d: Optional[int] = None
