"""Market trends endpoint."""
from __future__ import annotations

from typing import Any

from fastapi import APIRouter, Header, Query
from pydantic import BaseModel

from app.services.market_trends import get_market_trends

router = APIRouter(prefix="/api/v1/ml/market-trends", tags=["Market"])


class TrendItem(BaseModel):
    name: str
    tag: str
    growthPct: int


class MarketTrendsResponse(BaseModel):
    location: str
    trends: list[TrendItem]


@router.get("", response_model=MarketTrendsResponse)
def market_trends(
    location: str = Query(default="hcmc", description="Region code: hcmc | hanoi | danang"),
    x_tenant_id: str | None = Header(default=None, alias="X-Tenant-Id"),
) -> Any:
    """Return top trending fashion categories from Google Trends for the given region.
    Results are cached 24 h to avoid rate-limiting."""
    items = get_market_trends(location)
    return MarketTrendsResponse(
        location=location,
        trends=[TrendItem(**i) for i in items],
    )
