"""Market trend service — queries Google Trends via pytrends for VN fashion categories."""

from __future__ import annotations

import logging
import time
from typing import Any

import pandas as pd
from pytrends.request import TrendReq

logger = logging.getLogger(__name__)

_CACHE: dict[str, dict[str, Any]] = {}
_CACHE_TTL = 86_400  # 24 h — Google Trends data changes slowly

GEO_MAP = {"hcmc": "VN-SG", "hanoi": "VN-HN", "danang": "VN-DN"}
GEO_FALLBACK = "VN"

# Top categories to query — representative keyword first (same convention as build_base_model.py)
_CATEGORIES: list[tuple[str, str, str]] = [
    ("ao_so_mi", "Áo Sơ Mi", "Sơ mi"),
    ("vay_dam", "Váy Đầm", "Đầm"),
    ("ao_thun", "Áo Thun", "Áo phông"),
    ("ao_khoac", "Áo Khoác / Hoodie", "Khoác"),
    ("chan_vay", "Chân Váy", "Chân váy"),
    ("quan_jeans", "Quần Jean", "Denim"),
    ("dam_cong_so", "Thời Trang Công Sở", "Công sở"),
    ("tui_xach", "Túi Xách", "Phụ kiện"),
]

_KEYWORDS: dict[str, str] = {
    "ao_so_mi": "áo sơ mi",
    "vay_dam": "váy đầm",
    "ao_thun": "áo thun",
    "ao_khoac": "áo khoác",
    "chan_vay": "chân váy",
    "quan_jeans": "quần jean",
    "dam_cong_so": "thời trang công sở",
    "tui_xach": "túi xách",
}


def _fetch_trends(keywords: list[str], geo: str) -> pd.DataFrame:
    pt = TrendReq(hl="vi-VN", tz=420)
    pt.build_payload(keywords, timeframe="today 3-m", geo=geo)
    return pt.interest_over_time()


def _growth(series: pd.Series) -> int:
    """Compare last 30 days average vs previous 30 days. Returns integer %."""
    if len(series) < 2:
        return 0
    mid = len(series) // 2
    prev_avg = series.iloc[:mid].mean()
    curr_avg = series.iloc[mid:].mean()
    if prev_avg == 0:
        return 0
    return int(round((curr_avg - prev_avg) / prev_avg * 100))


def get_market_trends(location: str = "hcmc") -> list[dict[str, Any]]:
    """Return top trending fashion categories sorted by current interest descending."""
    cache_key = f"trends_{location}"
    cached = _CACHE.get(cache_key)
    if cached and time.time() - cached["ts"] < _CACHE_TTL:
        return cached["data"]

    geo = GEO_MAP.get(location, "VN-SG")
    results: list[dict[str, Any]] = []

    # Query in batches of 5 (pytrends limit)
    batch_keys = list(_KEYWORDS.keys())
    for i in range(0, len(batch_keys), 5):
        batch = batch_keys[i : i + 5]
        kws = [_KEYWORDS[k] for k in batch]
        try:
            df = _fetch_trends(kws, geo)
            if df is None or df.empty:
                df = _fetch_trends(kws, GEO_FALLBACK)
        except Exception as exc:
            logger.warning(
                "pytrends batch %s failed (geo=%s): %s — retrying with VN",
                batch,
                geo,
                exc,
            )
            try:
                df = _fetch_trends(kws, GEO_FALLBACK)
            except Exception as exc2:
                logger.error("pytrends batch %s failed on fallback: %s", batch, exc2)
                continue

        if "isPartial" in df.columns:
            df = df[df["isPartial"] == False].drop(columns=["isPartial"])  # noqa: E712

        for key, kw in zip(batch, kws):
            if kw not in df.columns:
                continue
            series = df[kw].dropna()
            current_interest = int(series.iloc[-4:].mean()) if len(series) >= 4 else 0
            growth = _growth(series)

            display = next((d for k, d, _ in _CATEGORIES if k == key), key)
            tag = next((t for k, _, t in _CATEGORIES if k == key), key)

            results.append(
                {
                    "name": display,
                    "tag": tag,
                    "growthPct": growth,
                    "_interest": current_interest,
                }
            )

    results.sort(key=lambda x: x["_interest"], reverse=True)
    for r in results:
        r.pop("_interest", None)

    _CACHE[cache_key] = {"data": results, "ts": time.time()}
    return results
