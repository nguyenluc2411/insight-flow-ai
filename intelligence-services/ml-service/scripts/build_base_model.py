"""Build per-category demand base models from Google Trends (Ho Chi Minh City).

Cold-start problem: a newly onboarded shop has no sales history, so a per-variant
Prophet model cannot be trained and the forecaster would otherwise return zeros.
This script pre-trains ONE Prophet model per fashion *category* on Google Trends
search interest for the HCM region (geo=VN-SG). Those base models capture the
seasonal SHAPE of demand and are used as the cold-start prior in
``app/services/forecasting.py`` (``ProphetForecaster._category_fallback``).

Run:
    python scripts/build_base_model.py

Data source: Google Trends via pytrends (geo=VN-SG)
  Choi, H. & Varian, H. (2012). "Predicting the Present with Google Trends."
    Economic Record, 88(S1), 2–9. doi:10.1111/j.1475-4932.2012.00809.x
  Kim, N. & Shamsollahi, Z. (2019). "Googling Fashion: Forecasting Fashion
    Consumer Behaviour Using Google Trends." Social Sciences, 8(4), 111.
    doi:10.3390/socsci8040111

Algorithm: Prophet
  Taylor, S.J. & Letham, B. (2018). "Forecasting at Scale." The American
    Statistician, 72(1), 37–45. doi:10.7287/peerj.preprints.3190v2

Why Prophet:
  - Built for daily/weekly series with weekly + yearly seasonality and holiday
    effects (exactly the structure of fashion demand).
  - Robust to missing data and outliers, which suits noisy Google Trends series.
  - Works well with only 1–2 years of data — matches the cold-start regime.
  - ``seasonality_mode="multiplicative"`` fits fashion: in-season demand scales
    up by a factor rather than adding a constant.

Why Google Trends:
  - Public, free, ~5 years of history.
  - ``geo='VN-SG'`` narrows the signal to the Ho Chi Minh City market.
  - Reflects consumer purchase intent ahead of realised sales.
  - A common cold-start proxy in the literature when no first-party POS data
    exists yet (Kim & Shamsollahi, 2019).

Limitations of Google Trends (IMPORTANT):
  - Relative search index (0–100), NOT absolute unit counts.
  - Unofficial API (pytrends) may rate-limit — collection is retried with a
    backoff and raises ``DataCollectionError`` on persistent failure.
  - Gives the seasonal SHAPE only, not the per-shop quantity. Absolute levels
    are calibrated later once a shop accumulates real POS sales.
"""
from __future__ import annotations

import json
import logging
import pickle
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

import pandas as pd
from prophet import Prophet
from pytrends.request import TrendReq

# Make the app package importable when run as a standalone script.
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.config import settings  # noqa: E402  (after sys.path tweak)
from app.utils.vn_holidays import VN_HOLIDAYS  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger("build_base_model")

# --- Collection / validation parameters ---
GEO = "VN-SG"  # Ho Chi Minh City region — do NOT use plain "VN"
TIMEFRAME = "today 5-y"  # 5 years to cover several seasonal cycles
MIN_DATA_POINTS = 104  # ~2 years of weekly Google Trends points
MAX_GAP_WEEKS = 8  # reject a series with > 8 consecutive empty weeks
RETRY_ATTEMPTS = 3
RETRY_SLEEP_SECONDS = 10

PAPER_CITATION = "Taylor & Letham (2018), doi:10.7287/peerj.preprints.3190v2"

# Fashion categories for the HCM market. Each value lists keyword variants; the
# FIRST keyword is used as the representative query for Google Trends.
FASHION_CATEGORIES: dict[str, list[str]] = {
    # --- Nhóm cơ bản ---
    "ao_so_mi":     ["áo sơ mi", "áo sơ mi nam", "áo sơ mi nữ"],
    "vay_dam":      ["váy đầm", "đầm dự tiệc", "váy midi"],
    "quan_jeans":   ["quần jeans", "quần denim"],
    "ao_thun":      ["áo thun", "áo phông"],
    "giay_sneaker": ["giày sneaker", "giày thể thao"],
    "ao_khoac":     ["áo khoác", "áo hoodie", "áo bomber"],
    "tui_xach":     ["túi xách", "túi tote", "túi đeo chéo"],
    "dam_maxi":     ["đầm maxi", "váy maxi", "đầm dài"],
    "quan_au":      ["quần âu nam", "quần tây"],
    "do_the_thao":  ["đồ thể thao", "quần short thể thao"],

    # --- Nhóm bổ sung đặc thù thị trường HCM ---
    # Nguồn: Vietnam e-commerce retail data Q3 2024
    # (sospgroup.com/post/expanding-your-retail-business-into-vietnam)
    # Women's fashion dẫn đầu thị trường VN: dresses, tops, shoes, accessories
    "dam_cong_so":  ["đầm công sở", "váy công sở", "áo kiểu công sở"],
    # Lý do: phân khúc lớn riêng biệt tại HCM, mùa vụ khác vay_dam tiệc;
    # tăng đầu năm (mùa tuyển dụng), giảm hè, tăng lại tháng 9-10.

    "ao_dai":       ["áo dài", "áo dài cách tân", "áo dài Tết"],
    # Lý do: đặc thù văn hóa Việt Nam, spike Tết cực mạnh (~300-500% vs baseline);
    # Prophet + VN_HOLIDAYS học pattern này rất tốt.
    # Nguồn: Shopee Vietnam top seller data (toplist.vn / tripi.vn)

    "chan_vay":     ["chân váy", "váy chữ A", "váy suông"],
    # Lý do: sản phẩm riêng biệt không nằm trong vay_dam; phổ biến Shopee HCM,
    # tăng mùa hè và công sở.

    "phu_kien":     ["phụ kiện thời trang", "thắt lưng nữ", "mũ thời trang"],
    # Lý do: chu kỳ mua khác quần áo, tăng mạnh dịp lễ và đầu hè.
    # Nguồn: Vietnam Clothing Market Size & Share Growth Report 2035
    # (expertmarketresearch.com)
}


class DataCollectionError(Exception):
    """Raised when Google Trends data cannot be collected after all retries."""


def collect_trends(category_key: str, keywords: list[str]) -> pd.DataFrame:
    """Fetch weekly Google Trends interest for a category's representative keyword.

    Args:
        category_key: Internal category identifier (used for logging).
        keywords: Keyword variants; only ``keywords[0]`` is queried.

    Returns:
        DataFrame with Prophet columns ``ds`` (date) and ``y`` (interest 0–100).

    Raises:
        DataCollectionError: If Trends returns nothing after ``RETRY_ATTEMPTS``.
    """
    keyword = keywords[0]
    pytrends = TrendReq(hl="vi-VN", tz=420)  # tz=420 → UTC+7 (Asia/Ho_Chi_Minh)
    last_error: Exception | None = None

    for attempt in range(1, RETRY_ATTEMPTS + 1):
        try:
            pytrends.build_payload([keyword], timeframe=TIMEFRAME, geo=GEO)
            raw = pytrends.interest_over_time()
            if raw is None or raw.empty:
                raise DataCollectionError(f"Empty Trends response for '{keyword}'")
            if "isPartial" in raw.columns:
                raw = raw[raw["isPartial"] == False].drop(columns=["isPartial"])  # noqa: E712
            series = pd.DataFrame(
                {
                    "ds": pd.to_datetime(raw.index),
                    "y": raw[keyword].astype(float).to_numpy(),
                }
            ).reset_index(drop=True)
            return series
        except Exception as exc:  # noqa: BLE001 — retry on any pytrends failure
            last_error = exc
            logger.warning(
                "[%s] Trends attempt %d/%d failed: %s",
                category_key, attempt, RETRY_ATTEMPTS, exc,
            )
            if attempt < RETRY_ATTEMPTS:
                time.sleep(RETRY_SLEEP_SECONDS)

    raise DataCollectionError(
        f"Failed to collect Google Trends for '{keyword}' (category={category_key}) "
        f"after {RETRY_ATTEMPTS} attempts: {last_error}"
    )


def validate_series(category_key: str, df: pd.DataFrame) -> bool:
    """Check a Trends series is rich enough to train on; never raises.

    Rejects the category (returns False, logs a warning) if there are fewer than
    ``MIN_DATA_POINTS`` points or a run of more than ``MAX_GAP_WEEKS`` consecutive
    empty (zero/NaN) weeks.
    """
    if len(df) < MIN_DATA_POINTS:
        logger.warning(
            "[%s] only %d data points (< %d) — skipping",
            category_key, len(df), MIN_DATA_POINTS,
        )
        return False

    gaps = (df["y"].fillna(0) <= 0).astype(int).tolist()
    max_run = 0
    run = 0
    for is_empty in gaps:
        run = run + 1 if is_empty else 0
        max_run = max(max_run, run)
    if max_run > MAX_GAP_WEEKS:
        logger.warning(
            "[%s] %d consecutive empty weeks (> %d) — skipping",
            category_key, max_run, MAX_GAP_WEEKS,
        )
        return False

    return True


def train_base_model(category_key: str, df: pd.DataFrame) -> None:
    """Train a category base Prophet model and persist it with metadata."""
    model = Prophet(
        yearly_seasonality=True,       # Bắt buộc: thời trang có chu kỳ năm rõ rệt
        weekly_seasonality=False,      # Tắt: data Google Trends là weekly, không cần
        daily_seasonality=False,       # Tắt: không có data daily
        seasonality_mode="multiplicative",  # Thời trang: nhu cầu nhân lên theo mùa
        holidays=VN_HOLIDAYS,          # Lễ/sale VN — xem app/utils/vn_holidays.py
        changepoint_prior_scale=0.05,  # Conservative: tránh overfit với data Trends
        seasonality_prior_scale=10,    # Default theo Taylor & Letham (2018)
    )
    model.fit(df)

    base_dir = Path(settings.MODEL_STORAGE_PATH) / "base"
    base_dir.mkdir(parents=True, exist_ok=True)

    model_path = base_dir / f"{category_key}.pkl"
    with model_path.open("wb") as f:
        pickle.dump(model, f)

    metadata = {
        "category": category_key,
        "trained_at": datetime.now(tz=timezone.utc).isoformat(),
        "data_points": int(len(df)),
        "date_range_start": df["ds"].min().date().isoformat(),
        "date_range_end": df["ds"].max().date().isoformat(),
        "geo": GEO,
        "source": "Google Trends (pytrends)",
        "algorithm": "Prophet",
        "paper_citation": PAPER_CITATION,
    }
    meta_path = base_dir / f"{category_key}.json"
    meta_path.write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8")

    logger.info(
        "[%s] base model saved → %s (%d points, %s → %s)",
        category_key, model_path, len(df),
        metadata["date_range_start"], metadata["date_range_end"],
    )


def main() -> int:
    """Build base models for every fashion category; resilient to per-category failures."""
    trained = skipped = failed = 0

    for category_key, keywords in FASHION_CATEGORIES.items():
        try:
            df = collect_trends(category_key, keywords)
        except DataCollectionError as exc:
            logger.error("[%s] data collection failed: %s", category_key, exc)
            failed += 1
            continue

        if not validate_series(category_key, df):
            skipped += 1
            continue

        try:
            train_base_model(category_key, df)
            trained += 1
        except Exception:  # noqa: BLE001 — one bad category must not abort the run
            logger.exception("[%s] training failed", category_key)
            failed += 1

        # Be gentle with the unofficial Trends endpoint between categories.
        time.sleep(RETRY_SLEEP_SECONDS)

    logger.info("Done. trained=%d skipped=%d failed=%d", trained, skipped, failed)
    return 0 if trained > 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
