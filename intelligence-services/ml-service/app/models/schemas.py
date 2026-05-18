"""Pydantic schemas for API request/response."""
from __future__ import annotations

from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, Field


# --- Forecast ---

class ForecastPoint(BaseModel):
    date: datetime
    predicted: float
    lower_bound: float = Field(..., alias="lowerBound")
    upper_bound: float = Field(..., alias="upperBound")

    class Config:
        populate_by_name = True


class ForecastResponse(BaseModel):
    variant_id: UUID = Field(..., alias="variantId")
    tenant_id: UUID = Field(..., alias="tenantId")
    forecast_days: int = Field(..., alias="forecastDays")
    confidence: Literal["high", "medium", "low", "none"]
    basis: Literal["variant", "category", "moving_average"]
    predictions: list[ForecastPoint]
    generated_at: datetime = Field(..., alias="generatedAt")

    class Config:
        populate_by_name = True


class BatchForecastRequest(BaseModel):
    variant_ids: list[UUID] = Field(..., alias="variantIds")
    days: int = Field(default=30, ge=1, le=90)

    class Config:
        populate_by_name = True


# --- Recommendation ---

class RecommendationResponse(BaseModel):
    id: UUID
    tenant_id: UUID = Field(..., alias="tenantId")
    variant_id: UUID = Field(..., alias="variantId")
    action: Literal["CLEARANCE", "RESTOCK", "PROMOTE", "OK"]
    reason: str | None = None
    priority: Literal["HIGH", "MEDIUM", "LOW"]
    suggested_discount_pct: float | None = Field(default=None, alias="suggestedDiscountPct")
    suggested_restock_qty: int | None = Field(default=None, alias="suggestedRestockQty")
    stock_age_days: int | None = Field(default=None, alias="stockAgeDays")
    current_stock: int | None = Field(default=None, alias="currentStock")
    sales_velocity_30d: float | None = Field(default=None, alias="salesVelocity30d")
    created_at: datetime = Field(..., alias="createdAt")

    class Config:
        populate_by_name = True
        from_attributes = True


class PagedRecommendationResponse(BaseModel):
    items: list[RecommendationResponse]
    page: int
    size: int
    total: int


class RefreshJobResponse(BaseModel):
    job_id: UUID = Field(..., alias="jobId")
    status: Literal["PENDING", "RUNNING", "SUCCESS", "FAILED"]

    class Config:
        populate_by_name = True


# --- Health ---

class HealthResponse(BaseModel):
    status: Literal["UP", "DOWN"]
    kafka_connected: bool = Field(..., alias="kafkaConnected")
    models_loaded: int = Field(..., alias="modelsLoaded")
    db_connected: bool = Field(..., alias="dbConnected")

    class Config:
        populate_by_name = True
