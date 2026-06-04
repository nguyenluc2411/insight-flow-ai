"""Gemini-based inventory-strategy advisor.

Folded in from the former Java recommendation-service (GeminiAiAdapter): given a
short inventory summary it asks Gemini for an inventory_strategy + trend_forecasting
JSON. Used by the file-upload flow, alongside the rule-based recommender.
"""

from __future__ import annotations

import logging
import re
import time

import httpx

from app.config import settings

logger = logging.getLogger(__name__)

_MAX_ATTEMPTS = 3
_RETRY_BACKOFF_SECONDS = 2

_PROMPT_TEMPLATE = (
    "Bạn là một Chuyên gia phân tích dữ liệu bán lẻ và Chiến lược gia chuỗi cung ứng thời trang.\n"
    "Điểm chất lượng dữ liệu: {score} (Nếu < 0.5, hãy cảnh báo gay gắt).\n"
    "Các trường dữ liệu bị thiếu: {missing}\n"
    "Dữ liệu báo cáo hàng tồn kho:\n{inventory}\n"
    "Nhiệm vụ của bạn là phân tích dữ liệu trên để đưa ra chiến lược tối ưu dòng vốn và dự báo xu hướng.\n"
    "RÀNG BUỘC TUYỆT ĐỐI: CHỈ trả về KẾT QUẢ DUY NHẤT LÀ MỘT CHUỖI JSON "
    "(không dùng markdown block, không giải thích), đúng cấu trúc sau:\n"
    "{{\n"
    '  "inventory_strategy": [\n'
    '    {{ "item_id_or_category": "...", "issue": "Tồn kho cao/Chậm luân chuyển", '
    '"action": "Xả hàng", "discount_percentage_recommendation": 30, '
    '"target_channel": "Sàn TMĐT/Cửa hàng Outlet", "reasoning": "..." }}\n'
    "  ],\n"
    '  "trend_forecasting": [\n'
    '    {{ "suggested_item": "Tên mặt hàng hot", "relevance_to_current_inventory": "...", '
    '"estimated_import_quantity": 150, "expected_retail_price_range": "...", '
    '"market_trend_reasoning": "..." }}\n'
    "  ]\n"
    "}}"
)


def generate_inventory_strategy(
    inventory_summary: str,
    completeness_score: float | None,
    missing_fields: str,
) -> str:
    """Call Gemini and return a clean JSON string. Raises on repeated failure."""
    if not settings.GEMINI_API_KEY:
        raise RuntimeError("GEMINI_API_KEY is not configured")

    score = completeness_score if completeness_score is not None else 0.0
    missing = missing_fields if missing_fields else "[]"
    prompt = _PROMPT_TEMPLATE.format(score=score, missing=missing, inventory=inventory_summary)

    body = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {"responseMimeType": "application/json"},
        "safetySettings": [
            {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_NONE"},
            {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_NONE"},
        ],
    }
    url = f"{settings.GEMINI_API_URL}?key={settings.GEMINI_API_KEY}"

    last_error: Exception | None = None
    for attempt in range(1, _MAX_ATTEMPTS + 1):
        try:
            logger.info("Calling Gemini for inventory strategy (attempt %d)", attempt)
            with httpx.Client(timeout=60.0) as client:
                resp = client.post(url, json=body)
                resp.raise_for_status()
                data = resp.json()

            candidates = data.get("candidates")
            if not candidates:
                raise RuntimeError("Gemini returned no candidates (possibly blocked)")
            raw_text = candidates[0]["content"]["parts"][0]["text"]
            return _extract_clean_json(raw_text)
        except Exception as exc:  # noqa: BLE001 — retry on any transient failure
            last_error = exc
            logger.warning("Gemini call failed (attempt %d): %s", attempt, exc)
            if attempt < _MAX_ATTEMPTS:
                time.sleep(_RETRY_BACKOFF_SECONDS)

    raise RuntimeError(f"Gemini call failed after {_MAX_ATTEMPTS} attempts: {last_error}")


_FENCED_JSON = re.compile(r"```(?:json)?\s*(\{.*\}|\[.*\])\s*```", re.DOTALL)


def _extract_clean_json(raw_output: str | None) -> str:
    """Pull a JSON object out of a possibly markdown-wrapped LLM response."""
    if not raw_output or not raw_output.strip():
        raise RuntimeError("AI returned an empty result")

    clean = raw_output.strip()
    match = _FENCED_JSON.search(clean)
    if match:
        return match.group(1).strip()

    start = clean.find("{")
    end = clean.rfind("}")
    if start != -1 and end != -1 and start < end:
        return clean[start : end + 1]

    return clean
