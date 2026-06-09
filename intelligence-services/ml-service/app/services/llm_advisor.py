"""Gemini-based inventory-strategy advisor.

Folded in from the former Java recommendation-service (GeminiAiAdapter): given a
short inventory summary it asks Gemini for an inventory_strategy + trend_forecasting
JSON. Used by the file-upload flow, alongside the rule-based recommender.
"""

from __future__ import annotations

import logging
import random
import re
import time

import httpx

from app.config import settings

logger = logging.getLogger(__name__)

_MAX_ATTEMPTS = 3  # per model, before falling back to the next model
_RETRY_BASE_SECONDS = 2.0
_RETRY_MAX_SECONDS = 30.0
# Transient statuses worth retrying (model overload / gateway / rate limit).
# 4xx like 400/401/403 mean a bad request or bad key — those won't self-heal,
# so we fail fast instead of burning the full retry budget.
_RETRYABLE_STATUS = {429, 500, 502, 503, 504}

_KEY_QUERY = re.compile(r"key=[\w-]+")


def _redact(text: str) -> str:
    """Strip the API key from any string before it reaches a log or DB error_log."""
    return _KEY_QUERY.sub("key=***", text)


def _model_label(base_url: str) -> str:
    """Pull the model id out of a Gemini endpoint URL for readable logs."""
    match = re.search(r"/models/([^:/?]+)", base_url)
    return match.group(1) if match else base_url


_PROMPT_TEMPLATE = (
    "Bạn là Chuyên gia phân tích dữ liệu bán lẻ và Chiến lược gia chuỗi cung ứng "
    "ngành THỜI TRANG, am hiểu sâu thị trường TP. Hồ Chí Minh, Việt Nam (gu ăn mặc, "
    "phân khúc giá, khí hậu nhiệt đới, mùa vụ và các dịp lễ/Tết ảnh hưởng tới nhu cầu).\n\n"
    "Điểm chất lượng dữ liệu: {score} (nếu < 0.5 hãy cảnh báo gay gắt về độ tin cậy của khuyến nghị).\n"
    "Các trường dữ liệu bị thiếu: {missing}\n\n"
    "DỮ LIỆU TỒN KHO THỰC TẾ (đã tổng hợp ở cấp SKU):\n{inventory}\n\n"
    "DÙNG GOOGLE TRENDS / GOOGLE SEARCH (qua công cụ tìm kiếm được cấp): hãy tra cứu "
    "mức độ quan tâm tìm kiếm và xu hướng thời trang ĐANG diễn ra tại TP.HCM / Việt Nam "
    "(từ khoá hot, kiểu dáng/chất liệu/màu đang lên, yếu tố mùa vụ và dịp lễ sắp tới) để "
    "đưa ra DỰ BÁO CÓ CĂN CỨ THỰC TẾ thay vì phỏng đoán. Khi có thể, dẫn lại tín hiệu cụ "
    'thể (ví dụ: từ khoá đang tăng) trong trường "trend_evidence".\n\n'
    "NHIỆM VỤ:\n"
    "1) Chiến lược tồn kho (inventory_strategy): phân tích theo TỪNG SKU/danh mục CỤ THỂ "
    "có trong dữ liệu trên (nêu đúng mã SKU + tên). Phân biệt rõ:\n"
    '   - Tồn rất CAO / chậm luân chuyển → action "Xả hàng" hoặc "Khuyến mãi", kèm '
    "discount_percentage_recommendation hợp lý.\n"
    '   - Tồn THẤP / sắp hết / hết hàng nhưng có dấu hiệu bán chạy → action "Nhập thêm", '
    "kèm suggested_restock_quantity.\n"
    '   - Gán priority "HIGH"/"MEDIUM"/"LOW" theo mức độ ảnh hưởng dòng vốn/doanh thu.\n'
    "2) Dự báo xu hướng (trend_forecasting): gợi ý mặt hàng NÊN nhập dựa trên xu hướng tra "
    "cứu được, ưu tiên thứ BỔ TRỢ cho tồn kho hiện có và TRÁNH trùng mặt hàng đang tồn cao.\n\n"
    "RÀNG BUỘC TUYỆT ĐỐI: CHỈ trả về KẾT QUẢ DUY NHẤT LÀ MỘT CHUỖI JSON hợp lệ "
    "(không dùng markdown block, không giải thích, không kèm trích dẫn), đúng cấu trúc sau "
    "(các trường tiền tệ ghi bằng VNĐ):\n"
    "{{\n"
    '  "inventory_strategy": [\n'
    '    {{ "item_id_or_category": "QJ-1006 Quần Jean Skinny", "issue": "Tồn kho cao/Chậm luân chuyển", '
    '"action": "Xả hàng", "priority": "HIGH", "discount_percentage_recommendation": 30, '
    '"suggested_restock_quantity": null, "target_channel": "Sàn TMĐT/Cửa hàng Outlet", "reasoning": "..." }}\n'
    "  ],\n"
    '  "trend_forecasting": [\n'
    '    {{ "suggested_item": "Tên mặt hàng hot", "relevance_to_current_inventory": "...", '
    '"estimated_import_quantity": 150, "expected_retail_price_range": "450.000 - 850.000 VNĐ", '
    '"trend_evidence": "Tín hiệu từ Google Trends/tìm kiếm tại TP.HCM", "market_trend_reasoning": "..." }}\n'
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
    prompt = _PROMPT_TEMPLATE.format(
        score=score, missing=missing, inventory=inventory_summary
    )

    body: dict = {
        "contents": [{"parts": [{"text": prompt}]}],
        "safetySettings": [
            {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_NONE"},
            {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_NONE"},
        ],
    }
    if settings.GEMINI_USE_SEARCH_GROUNDING:
        # Grounding tool can't be combined with responseMimeType=application/json,
        # so we ask for JSON in the prompt and recover it via _extract_clean_json.
        body["tools"] = [{"google_search": {}}]
    else:
        body["generationConfig"] = {"responseMimeType": "application/json"}
    # Try the primary model first; if it stays overloaded (503/429) across all
    # attempts, degrade to the fallback model rather than failing the workspace.
    endpoints = [settings.GEMINI_API_URL]
    fallback = settings.GEMINI_FALLBACK_API_URL
    if fallback and fallback != settings.GEMINI_API_URL:
        endpoints.append(fallback)

    last_error: Exception | None = None
    for base_url in endpoints:
        model = _model_label(base_url)
        url = f"{base_url}?key={settings.GEMINI_API_KEY}"
        for attempt in range(1, _MAX_ATTEMPTS + 1):
            try:
                logger.info(
                    "Calling Gemini %s (attempt %d/%d)", model, attempt, _MAX_ATTEMPTS
                )
                with httpx.Client(timeout=60.0) as client:
                    resp = client.post(url, json=body)
                    resp.raise_for_status()
                    data = resp.json()

                candidates = data.get("candidates")
                if not candidates:
                    raise RuntimeError(
                        "Gemini returned no candidates (possibly blocked)"
                    )
                # With Google Search grounding the answer can be split across several
                # parts (and some parts carry no "text"), so join every text part.
                parts = candidates[0].get("content", {}).get("parts", [])
                raw_text = "".join(
                    p["text"] for p in parts if isinstance(p, dict) and "text" in p
                )
                return _extract_clean_json(raw_text)
            except httpx.HTTPStatusError as exc:
                status = exc.response.status_code
                last_error = exc
                if status not in _RETRYABLE_STATUS:
                    # Bad key / bad request — won't self-heal and won't differ across
                    # models. Surface immediately, but never leak the key in the message.
                    raise RuntimeError(
                        f"Gemini {model} returned non-retryable HTTP {status}"
                    ) from None
                logger.warning(
                    "Gemini %s transient HTTP %d (attempt %d/%d)",
                    model,
                    status,
                    attempt,
                    _MAX_ATTEMPTS,
                )
            except httpx.RequestError as exc:
                # Network / timeout / connection reset — transient, retry.
                last_error = exc
                logger.warning(
                    "Gemini %s network error (attempt %d/%d): %s",
                    model,
                    attempt,
                    _MAX_ATTEMPTS,
                    _redact(str(exc)),
                )
            except (
                Exception
            ) as exc:  # noqa: BLE001 — empty/blocked/parse, retry once more
                last_error = exc
                logger.warning(
                    "Gemini %s call failed (attempt %d/%d): %s",
                    model,
                    attempt,
                    _MAX_ATTEMPTS,
                    _redact(str(exc)),
                )

            if attempt < _MAX_ATTEMPTS:
                # Exponential backoff (2s, 4s, 8s...) capped, plus jitter so parallel
                # workspaces don't all retry in lockstep against an overloaded API.
                backoff = min(
                    _RETRY_BASE_SECONDS * 2 ** (attempt - 1), _RETRY_MAX_SECONDS
                )
                time.sleep(backoff + random.uniform(0, backoff * 0.25))

        logger.warning(
            "Gemini model %s exhausted; trying fallback model if available", model
        )

    raise RuntimeError(f"Gemini call failed for all models: {_redact(str(last_error))}")


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
