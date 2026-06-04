"""Orchestrates the LLM inventory-advice flow for the file-upload pipeline.

On inventory.ingestion.completed: fetch the parsed inventory snapshot from
data-ingestion-service, summarize it, ask Gemini for a strategy, persist the
result, and emit inventory.recommendation.generated / .failed.
"""

from __future__ import annotations

import logging
from datetime import datetime, timezone
from uuid import UUID

import httpx
from sqlalchemy.orm import Session

from app.config import settings
from app.db.database import SessionLocal
from app.db.models import InventoryAdvice
from app.events import producer
from app.services import llm_advisor

logger = logging.getLogger(__name__)


def get_advice_by_workspace(
    db: Session, tenant_id: UUID, workspace_id: str
) -> InventoryAdvice | None:
    return (
        db.query(InventoryAdvice)
        .filter(
            InventoryAdvice.tenant_id == tenant_id,
            InventoryAdvice.workspace_id == workspace_id,
        )
        .first()
    )


def process_ingestion_completed(
    tenant_id: str,
    workspace_id: str,
    completeness_score: float | None,
    missing_fields: list[str] | None,
) -> None:
    """Run the LLM advisor for one workspace. Safe to call from a worker thread."""
    session = SessionLocal()
    try:
        tenant_uuid = UUID(tenant_id)
        advice = (
            session.query(InventoryAdvice)
            .filter(InventoryAdvice.workspace_id == workspace_id)
            .first()
        )
        if advice is not None and advice.status == "PROCESSING":
            logger.warning("Workspace %s already being processed by the advisor", workspace_id)
            return

        if advice is None:
            advice = InventoryAdvice(
                tenant_id=tenant_uuid,
                workspace_id=workspace_id,
                status="PROCESSING",
            )
            session.add(advice)
        else:
            advice.status = "PROCESSING"
            advice.error_log = None
        advice.updated_at = datetime.now(tz=timezone.utc)
        session.commit()
    except Exception:  # noqa: BLE001
        logger.error("Failed to mark advice PROCESSING for %s", workspace_id, exc_info=True)
        session.rollback()
        session.close()
        return

    try:
        inventory = _fetch_inventory(tenant_id, workspace_id)
        products = inventory.get("products") or []
        if not products:
            raise RuntimeError("Empty inventory snapshot from data-ingestion")

        summary = _summarize(inventory)
        missing_str = str(missing_fields) if missing_fields else "[]"
        result_json = llm_advisor.generate_inventory_strategy(
            summary, completeness_score, missing_str
        )

        advice.result_json = result_json
        advice.status = "DONE"
        advice.updated_at = datetime.now(tz=timezone.utc)
        session.commit()
        producer.send_recommendation_generated(workspace_id, tenant_id)
        logger.info("LLM advice DONE for workspace %s", workspace_id)
    except Exception as exc:  # noqa: BLE001
        logger.error("LLM advice FAILED for workspace %s", workspace_id, exc_info=True)
        try:
            advice.status = "ERROR"
            advice.error_log = str(exc)
            advice.updated_at = datetime.now(tz=timezone.utc)
            session.commit()
        except Exception:  # noqa: BLE001
            session.rollback()
        producer.send_recommendation_failed(workspace_id, str(exc))
    finally:
        session.close()


def _fetch_inventory(tenant_id: str, workspace_id: str) -> dict:
    """Pull the parsed inventory snapshot from data-ingestion (tenant-scoped)."""
    url = f"{settings.DATA_INGESTION_URL}/api/v1/inventories/workspace/{workspace_id}"
    with httpx.Client(timeout=30.0) as client:
        resp = client.get(url, headers={"X-Tenant-Id": tenant_id})
        resp.raise_for_status()
        return resp.json()


def _summarize(inventory: dict) -> str:
    products = inventory.get("products") or []
    variants = inventory.get("variants") or []
    facts = inventory.get("inventoryFacts") or []
    total_stock = sum(int(f.get("quantityInStock") or 0) for f in facts)
    return (
        f"Tổng sản phẩm: {len(products)}. Tổng SKU: {len(variants)}. "
        f"Tổng lượng tồn kho: {total_stock} chiếc."
    )
