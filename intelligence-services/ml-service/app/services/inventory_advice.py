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


# How many SKUs to surface per bucket, and the stock level under which a SKU is
# treated as a near-stockout / restock candidate.
_TOP_N = 12
_LOW_STOCK_MAX = 15


def _num(value) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0


def _get(d: dict, *keys):
    """First non-None value among snake_case/camelCase key variants."""
    for k in keys:
        if d.get(k) is not None:
            return d.get(k)
    return None


def _age_days(value) -> int | None:
    """Days between an ISO date string (YYYY-MM-DD...) and today, or None."""
    if not value:
        return None
    try:
        d = datetime.fromisoformat(str(value)[:10]).date()
    except ValueError:
        return None
    return (datetime.now(tz=timezone.utc).date() - d).days


def _vnd(value: float) -> str:
    """Format a number as VND with '.' thousand separators (1000000 -> 1.000.000đ)."""
    return f"{int(round(value)):,}đ".replace(",", ".")


def _summarize(inventory: dict) -> str:
    """Build a decision-focused, SKU-level briefing for the LLM advisor.

    The old version collapsed the whole catalog into three aggregate numbers, so
    the model could only answer in generalities and never saw which products were
    overstocked vs. sold out. Here we join every inventory fact up to its variant
    and product, then surface named SKUs, prices/margins and per-category roll-ups
    so the advisor can name real items and tell "xả hàng" apart from "nhập thêm".

    data-ingestion serializes JSON in snake_case (JacksonConfig); camelCase keys
    are kept as a fallback in case the naming strategy ever changes.
    """
    products = inventory.get("products") or []
    variants = inventory.get("variants") or []
    facts = _get(inventory, "inventory_facts", "inventoryFacts") or []

    product_by_id = {p.get("id"): p for p in products}
    variant_by_id = {v.get("id"): v for v in variants}

    records: list[dict] = []
    for f in facts:
        variant = variant_by_id.get(_get(f, "variant_id", "variantId")) or {}
        product = product_by_id.get(_get(variant, "product_id", "productId")) or {}
        stock = int(_num(_get(f, "quantity_in_stock", "quantityInStock")))
        sold_raw = _get(f, "quantity_sold", "quantitySold")
        cost = _num(_get(f, "cost_price", "costPrice"))
        retail = _num(_get(f, "retail_price", "retailPrice"))
        records.append(
            {
                "sku": _get(variant, "sku") or _get(product, "product_code", "productCode") or "N/A",
                "name": _get(product, "product_name", "productName") or "Không tên",
                "category": _get(product, "category") or "Khác",
                "stock": stock,
                "sold": int(_num(sold_raw)) if sold_raw is not None else None,
                "retail": retail,
                "margin_pct": round((retail - cost) / retail * 100) if retail > 0 else None,
                "stock_age": _age_days(_get(f, "import_date", "importDate")),
            }
        )

    if not records:
        return (
            f"Tổng sản phẩm: {len(products)}. Tổng SKU: {len(variants)}. "
            f"Không có dòng tồn kho nào để phân tích."
        )

    total_stock = sum(r["stock"] for r in records)
    total_value = sum(r["stock"] * (r["retail"] or 0) for r in records)
    has_sales = any(r["sold"] is not None for r in records)

    # Per-category roll-up (stock + average margin), heaviest categories first.
    cats: dict[str, dict] = {}
    for r in records:
        c = cats.setdefault(r["category"], {"skus": 0, "stock": 0, "margins": []})
        c["skus"] += 1
        c["stock"] += r["stock"]
        if r["margin_pct"] is not None:
            c["margins"].append(r["margin_pct"])
    cat_lines = []
    for name, c in sorted(cats.items(), key=lambda kv: kv[1]["stock"], reverse=True):
        avg_margin = round(sum(c["margins"]) / len(c["margins"])) if c["margins"] else None
        margin_txt = f", biên LN TB ~{avg_margin}%" if avg_margin is not None else ""
        cat_lines.append(f"- {name}: {c['skus']} SKU, tồn {c['stock']} chiếc{margin_txt}")

    def _line(r: dict) -> str:
        parts = [f"{r['sku']} {r['name']}", f"[{r['category']}]", f"tồn {r['stock']}"]
        if r["sold"] is not None:
            parts.append(f"đã bán {r['sold']}")
        if r["retail"]:
            parts.append(f"giá {_vnd(r['retail'])}")
        if r["margin_pct"] is not None:
            parts.append(f"biên {r['margin_pct']}%")
        if r["stock_age"] is not None:
            parts.append(f"nhập {r['stock_age']} ngày trước")
        return " | ".join(parts)

    overstock = sorted(records, key=lambda r: r["stock"], reverse=True)[:_TOP_N]
    low_sorted = sorted(records, key=lambda r: r["stock"])
    low_stock = [r for r in low_sorted if r["stock"] <= _LOW_STOCK_MAX] or low_sorted[:5]

    sales_note = (
        "Có dữ liệu số lượng đã bán — hãy ưu tiên dùng nó để đánh giá tốc độ luân chuyển."
        if has_sales
        else (
            "KHÔNG có dữ liệu số lượng đã bán / ngày bán cuối → hãy suy luận dựa trên mức tồn kho: "
            "tồn rất cao = ứ đọng/chậm luân chuyển; tồn ~0 = nhiều khả năng bán chạy đã cạn hàng, cần nhập gấp."
        )
    )

    return (
        f"TỔNG QUAN: {len(products)} sản phẩm | {len(variants)} SKU | "
        f"tổng tồn {total_stock} chiếc | tổng giá trị tồn (theo giá bán lẻ) ~{_vnd(total_value)}.\n"
        f"DỮ LIỆU BÁN: {sales_note}\n\n"
        "TỒN KHO THEO DANH MỤC:\n" + "\n".join(cat_lines) + "\n\n"
        "NHÓM TỒN CAO NHẤT (ứng viên XẢ HÀNG / chậm luân chuyển):\n"
        + "\n".join(_line(r) for r in overstock) + "\n\n"
        "NHÓM TỒN THẤP / SẮP HẾT (ứng viên NHẬP THÊM nếu đang bán chạy):\n"
        + "\n".join(_line(r) for r in low_stock)
    )
