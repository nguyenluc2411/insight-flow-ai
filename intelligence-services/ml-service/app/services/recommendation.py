"""Rule-based recommendation engine for inventory actions."""

from __future__ import annotations

import logging
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Literal
from uuid import UUID

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.db.models import InventorySnapshot, Recommendation, SalesData
from app.services.forecasting import forecaster

logger = logging.getLogger(__name__)

Action = Literal["CLEARANCE", "RESTOCK", "PROMOTE", "OK"]
Priority = Literal["HIGH", "MEDIUM", "LOW"]


@dataclass
class RuleResult:
    action: Action
    priority: Priority
    reason: str
    suggested_discount_pct: float | None = None
    suggested_restock_qty: int | None = None


class RuleBasedRecommender:
    """Rules:
    - CLEARANCE: stock_age > 90 days AND velocity_30d < velocity_90d * 0.5
    - RESTOCK:   days_remaining < 14 AND forecast_30d > current_stock
    - PROMOTE:   forecast_30d > velocity_30d*30 * 1.5 AND days_remaining > 14
    - OK:        default
    """

    def calculate_sales_velocity(
        self,
        db: Session,
        tenant_id: UUID,
        variant_id: UUID,
        days: int,
    ) -> float:
        cutoff = datetime.now(tz=timezone.utc) - timedelta(days=days)
        total = (
            db.query(func.sum(SalesData.quantity))
            .filter(
                SalesData.tenant_id == tenant_id,
                SalesData.variant_id == variant_id,
                SalesData.occurred_at >= cutoff,
            )
            .scalar()
        )
        if total is None:
            return 0.0
        return float(total) / max(days, 1)

    def calculate_stock_age(
        self, db: Session, tenant_id: UUID, variant_id: UUID
    ) -> int:
        snap = (
            db.query(InventorySnapshot)
            .filter(
                InventorySnapshot.tenant_id == tenant_id,
                InventorySnapshot.variant_id == variant_id,
            )
            .order_by(InventorySnapshot.first_restocked_at.asc())
            .first()
        )
        if snap is None or snap.first_restocked_at is None:
            return 0
        delta = datetime.now(tz=timezone.utc) - snap.first_restocked_at
        return max(0, delta.days)

    def get_current_stock(self, db: Session, tenant_id: UUID, variant_id: UUID) -> int:
        total = (
            db.query(func.sum(InventorySnapshot.quantity))
            .filter(
                InventorySnapshot.tenant_id == tenant_id,
                InventorySnapshot.variant_id == variant_id,
            )
            .scalar()
        )
        return int(total) if total else 0

    def apply_rules(
        self,
        stock_age: int,
        velocity_30d: float,
        velocity_90d: float,
        current_stock: int,
        forecast_30d: float,
    ) -> RuleResult:
        if stock_age > 90 and velocity_90d > 0 and velocity_30d < velocity_90d * 0.5:
            discount = 50.0 if stock_age > 120 else 30.0
            priority: Priority = "HIGH" if stock_age > 120 else "MEDIUM"
            reduction_pct = int((1 - velocity_30d / velocity_90d) * 100)
            return RuleResult(
                action="CLEARANCE",
                priority=priority,
                reason=f"Tồn {stock_age} ngày, tốc độ bán giảm {reduction_pct}%",
                suggested_discount_pct=discount,
            )

        days_remaining = (current_stock / velocity_30d) if velocity_30d > 0 else 999.0

        if days_remaining < 14 and forecast_30d > current_stock:
            qty = max(0, int(forecast_30d - current_stock * 1.2))
            priority = "HIGH" if days_remaining < 7 else "MEDIUM"
            return RuleResult(
                action="RESTOCK",
                priority=priority,
                reason=(
                    f"Còn {days_remaining:.0f} ngày hàng, dự báo cần {forecast_30d:.0f} sản phẩm"
                ),
                suggested_restock_qty=qty,
            )

        if (
            velocity_30d > 0
            and forecast_30d > velocity_30d * 30 * 1.5
            and days_remaining > 14
        ):
            increase_pct = int((forecast_30d / (velocity_30d * 30) - 1) * 100)
            return RuleResult(
                action="PROMOTE",
                priority="MEDIUM",
                reason=f"Dự báo tăng {increase_pct}% so với trung bình",
            )

        return RuleResult(action="OK", priority="LOW", reason="Tồn kho ổn định")

    def generate_for_tenant(self, db: Session, tenant_id: UUID) -> list[Recommendation]:
        """Run rules for all variants this tenant has data for; persist results."""
        variant_ids = (
            db.query(SalesData.variant_id)
            .filter(SalesData.tenant_id == tenant_id)
            .distinct()
            .all()
        )
        variant_ids = [v[0] for v in variant_ids]
        # Also include variants we only know about via inventory snapshots
        snap_variants = (
            db.query(InventorySnapshot.variant_id)
            .filter(InventorySnapshot.tenant_id == tenant_id)
            .distinct()
            .all()
        )
        for sv in snap_variants:
            if sv[0] not in variant_ids:
                variant_ids.append(sv[0])

        results: list[Recommendation] = []
        for variant_id in variant_ids:
            velocity_30 = self.calculate_sales_velocity(db, tenant_id, variant_id, 30)
            velocity_90 = self.calculate_sales_velocity(db, tenant_id, variant_id, 90)
            stock_age = self.calculate_stock_age(db, tenant_id, variant_id)
            current_stock = self.get_current_stock(db, tenant_id, variant_id)

            forecast_points, _confidence, _basis = forecaster.predict(
                db, tenant_id, variant_id, days=30
            )
            forecast_30d = sum(p.predicted for p in forecast_points)

            rule = self.apply_rules(
                stock_age=stock_age,
                velocity_30d=velocity_30,
                velocity_90d=velocity_90,
                current_stock=current_stock,
                forecast_30d=forecast_30d,
            )

            rec = Recommendation(
                tenant_id=tenant_id,
                variant_id=variant_id,
                action=rule.action,
                reason=rule.reason,
                priority=rule.priority,
                suggested_discount_pct=rule.suggested_discount_pct,
                suggested_restock_qty=rule.suggested_restock_qty,
                stock_age_days=stock_age,
                current_stock=current_stock,
                sales_velocity_30d=velocity_30,
            )
            db.add(rec)
            results.append(rec)

        db.commit()
        logger.info(
            "Generated %d recommendations for tenant=%s", len(results), tenant_id
        )
        return results


recommender = RuleBasedRecommender()
