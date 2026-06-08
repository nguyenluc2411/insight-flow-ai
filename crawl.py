"""
crawl.py
--------
Crawl Google Trends daily data bằng cách chia nhỏ thành các khoảng 6 tháng,
ghép lại và chuẩn hoá về cùng scale (0-100) dùng khoảng overlap.

Phiên bản hoàn thiện:
- Tự động sinh keyword từ related_queries() của Google Trends
- Map keyword về standard_value qua catalog_dictionaries
- Export trend_report.csv với đầy đủ attr_ columns cho train.py
- Phát hiện emerging trend (tăng >30% so với baseline 6 tháng)
- Checkpoint từng keyword → không mất data khi crash giữa chừng
- Progress log chi tiết: ETA, % hoàn thành
- Tối ưu RAM 16GB: in-memory processing, intelligent sleep
"""

import time
import random
import logging
import json
import pandas as pd
from datetime import date, timedelta
from pathlib import Path
from pytrends.request import TrendReq

# ── Config ────────────────────────────────────────────────────────────────────

SEED_KEYWORDS = [
    "áo thun nam",
    "quần jean nữ",
    "giày sneaker",
    "váy nữ",
    "áo khoác",
    "quần short nam",
    "sandal nữ",
    "túi xách nữ",
    "áo sơ mi nam",
    "đầm nữ",
]

MANUAL_KEYWORDS = [
    "ao phong oversize",
    "quan jean nu",
    "vay hoa",
    "ao so mi nam",
    "giay sneaker",
    "tui xach nu",
    "dam maxi",
    "ao khoac jean",
    "quan short nam",
    "sandal nu",
]

# Catalog dictionary — map synonym → attributes
# Nguồn sự thật duy nhất, dùng chung cho crawl.py và train.py
CATALOG_DICT: dict[str, dict[str, str]] = {
    # TARGET_DEMOGRAPHIC
    "nam":      {"attribute_type": "TARGET_DEMOGRAPHIC", "standard_value": "Men"},
    "men":      {"attribute_type": "TARGET_DEMOGRAPHIC", "standard_value": "Men"},
    "con trai": {"attribute_type": "TARGET_DEMOGRAPHIC", "standard_value": "Men"},
    "nữ":       {"attribute_type": "TARGET_DEMOGRAPHIC", "standard_value": "Women"},
    "nu":       {"attribute_type": "TARGET_DEMOGRAPHIC", "standard_value": "Women"},
    "women":    {"attribute_type": "TARGET_DEMOGRAPHIC", "standard_value": "Women"},
    "con gái":  {"attribute_type": "TARGET_DEMOGRAPHIC", "standard_value": "Women"},
    "trẻ em":   {"attribute_type": "TARGET_DEMOGRAPHIC", "standard_value": "Kids"},
    "unisex":   {"attribute_type": "TARGET_DEMOGRAPHIC", "standard_value": "Unisex"},
    # CATEGORY
    "áo":       {"attribute_type": "CATEGORY", "standard_value": "Top"},
    "quần":     {"attribute_type": "CATEGORY", "standard_value": "Bottom"},
    "váy":      {"attribute_type": "CATEGORY", "standard_value": "Bottom"},
    "khoác":    {"attribute_type": "CATEGORY", "standard_value": "Outerwear"},
    "đầm":      {"attribute_type": "CATEGORY", "standard_value": "Dress"},
    "dam":      {"attribute_type": "CATEGORY", "standard_value": "Dress"},
    "giày":     {"attribute_type": "CATEGORY", "standard_value": "Footwear"},
    "giay":     {"attribute_type": "CATEGORY", "standard_value": "Footwear"},
    "dép":      {"attribute_type": "CATEGORY", "standard_value": "Footwear"},
    # SUB_CATEGORY
    "áo thun":  {"attribute_type": "SUB_CATEGORY", "standard_value": "T-Shirt"},
    "ao thun":  {"attribute_type": "SUB_CATEGORY", "standard_value": "T-Shirt"},
    "áo phông": {"attribute_type": "SUB_CATEGORY", "standard_value": "T-Shirt"},
    "ao phong": {"attribute_type": "SUB_CATEGORY", "standard_value": "T-Shirt"},
    "tee":      {"attribute_type": "SUB_CATEGORY", "standard_value": "T-Shirt"},
    "sơ mi":    {"attribute_type": "SUB_CATEGORY", "standard_value": "Shirt"},
    "so mi":    {"attribute_type": "SUB_CATEGORY", "standard_value": "Shirt"},
    "polo":     {"attribute_type": "SUB_CATEGORY", "standard_value": "Polo"},
    "ba lỗ":    {"attribute_type": "SUB_CATEGORY", "standard_value": "Tank Top"},
    "áo dây":   {"attribute_type": "SUB_CATEGORY", "standard_value": "Tank Top"},
    "hoodie":   {"attribute_type": "SUB_CATEGORY", "standard_value": "Hoodie"},
    "jacket":   {"attribute_type": "SUB_CATEGORY", "standard_value": "Jacket"},
    "áo gió":   {"attribute_type": "SUB_CATEGORY", "standard_value": "Jacket"},
    "ao khoac": {"attribute_type": "SUB_CATEGORY", "standard_value": "Jacket"},
    "blazer":   {"attribute_type": "SUB_CATEGORY", "standard_value": "Blazer"},
    "jean":     {"attribute_type": "SUB_CATEGORY", "standard_value": "Jeans"},
    "quần bò":  {"attribute_type": "SUB_CATEGORY", "standard_value": "Jeans"},
    "quan jean":{"attribute_type": "SUB_CATEGORY", "standard_value": "Jeans"},
    "quần tây": {"attribute_type": "SUB_CATEGORY", "standard_value": "Trousers"},
    "short":    {"attribute_type": "SUB_CATEGORY", "standard_value": "Shorts"},
    "quan short":{"attribute_type": "SUB_CATEGORY", "standard_value": "Shorts"},
    "quần đùi": {"attribute_type": "SUB_CATEGORY", "standard_value": "Shorts"},
    "cargo":    {"attribute_type": "SUB_CATEGORY", "standard_value": "Cargo"},
    "jogger":   {"attribute_type": "SUB_CATEGORY", "standard_value": "Jogger"},
    "chân váy": {"attribute_type": "SUB_CATEGORY", "standard_value": "Skirt"},
    "chan vay":  {"attribute_type": "SUB_CATEGORY", "standard_value": "Skirt"},
    "váy liền": {"attribute_type": "SUB_CATEGORY", "standard_value": "Dress"},
    "sneaker":  {"attribute_type": "SUB_CATEGORY", "standard_value": "Sneaker"},
    "sandal":   {"attribute_type": "SUB_CATEGORY", "standard_value": "Sandal"},
    "sandal nu":{"attribute_type": "SUB_CATEGORY", "standard_value": "Sandal"},
    "tui xach": {"attribute_type": "SUB_CATEGORY", "standard_value": "Handbag"},
    "túi xách": {"attribute_type": "SUB_CATEGORY", "standard_value": "Handbag"},
    # FIT_TYPE
    "oversize":  {"attribute_type": "FIT_TYPE", "standard_value": "Oversize"},
    "form rộng": {"attribute_type": "FIT_TYPE", "standard_value": "Oversize"},
    "phom rong": {"attribute_type": "FIT_TYPE", "standard_value": "Oversize"},
    "slim":      {"attribute_type": "FIT_TYPE", "standard_value": "Slim-fit"},
    "ôm":        {"attribute_type": "FIT_TYPE", "standard_value": "Slim-fit"},
    "skinny":    {"attribute_type": "FIT_TYPE", "standard_value": "Skinny"},
    "regular":   {"attribute_type": "FIT_TYPE", "standard_value": "Regular"},
    "relaxed":   {"attribute_type": "FIT_TYPE", "standard_value": "Relaxed"},
    # PATTERN
    "hoa":      {"attribute_type": "PATTERN", "standard_value": "Floral"},
    "vay hoa":  {"attribute_type": "PATTERN", "standard_value": "Floral"},
    "kẻ sọc":   {"attribute_type": "PATTERN", "standard_value": "Striped"},
    "sọc":      {"attribute_type": "PATTERN", "standard_value": "Striped"},
    "caro":     {"attribute_type": "PATTERN", "standard_value": "Plaid"},
    "trơn":     {"attribute_type": "PATTERN", "standard_value": "Solid"},
    "in hình":  {"attribute_type": "PATTERN", "standard_value": "Graphic"},
    # MATERIAL
    "cotton":   {"attribute_type": "MATERIAL", "standard_value": "Cotton"},
    "denim":    {"attribute_type": "MATERIAL", "standard_value": "Denim"},
    "linen":    {"attribute_type": "MATERIAL", "standard_value": "Linen"},
    "đũi":      {"attribute_type": "MATERIAL", "standard_value": "Linen"},
    "kaki":     {"attribute_type": "MATERIAL", "standard_value": "Khaki"},
    "len":      {"attribute_type": "MATERIAL", "standard_value": "Wool"},
    "da":       {"attribute_type": "MATERIAL", "standard_value": "Leather"},
}

GEO                      = "VN"
START_DATE               = date(2021, 1, 1)
END_DATE                 = date.today()
WINDOW_MONTHS            = 6
OVERLAP_DAYS             = 30
OUTPUT                   = Path("./output/google_trends_raw.csv")
CHECKPOINT_DIR           = Path("./output/checkpoints")
RELATED_CACHE            = Path("./output/related_queries_cache.json")
TREND_REPORT_PATH        = Path("./output/trend_report.csv")

MIN_KEYWORD_WORDS        = 2
MAX_KEYWORD_WORDS        = 6
MAX_RELATED_PER_SEED     = 15

EMERGING_TREND_WINDOW    = 4
EMERGING_TREND_BASELINE  = 26
EMERGING_TREND_THRESHOLD = 0.30

# ── Logging ───────────────────────────────────────────────────────────────────

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(message)s",
    datefmt="%Y-%m-%dT%H:%M:%S",
)
logger = logging.getLogger(__name__)

# ── Catalog helpers ───────────────────────────────────────────────────────────

def map_keyword_to_attributes(keyword: str) -> dict[str, str]:
    """
    Map keyword về các standard_value trong CATALOG_DICT.
    Thử match cụm 2 từ trước rồi mới match đơn từ.
    Trả về dict: { ATTRIBUTE_TYPE: standard_value, ... }
    """
    tokens = keyword.lower().strip().split()
    attributes: dict[str, str] = {}

    for i in range(len(tokens)):
        for length in [2, 1]:
            if i + length <= len(tokens):
                phrase = " ".join(tokens[i:i + length])
                if phrase in CATALOG_DICT:
                    entry     = CATALOG_DICT[phrase]
                    attr_type = entry["attribute_type"]
                    if attr_type not in attributes:
                        attributes[attr_type] = entry["standard_value"]
                    break
    return attributes


def assign_cluster(keyword: str, df_kw: pd.DataFrame | None = None) -> str:
    """
    Gán cluster dựa trên attributes từ catalog + đặc điểm data thực tế.
    Trả về: 'HIGH_SEASONAL' | 'MODERATE' | 'STABLE'
    """
    attrs = map_keyword_to_attributes(keyword)

    # Rule-based từ catalog attributes
    if attrs.get("PATTERN") == "Floral":
        return "HIGH_SEASONAL"
    if attrs.get("SUB_CATEGORY") in {"Skirt", "Dress", "Sandal"}:
        return "HIGH_SEASONAL"
    if attrs.get("SUB_CATEGORY") in {"Shirt", "Sneaker", "Shorts", "Jacket"}:
        return "MODERATE"
    if attrs.get("FIT_TYPE") == "Oversize":
        return "STABLE"

    # Data-driven nếu đã có data
    if df_kw is not None and len(df_kw) >= 60:
        y = df_kw["value"]
        seasonality_strength = y.std() / (y.mean() + 1e-9)
        spike_ratio          = y.quantile(0.95) / (y.quantile(0.50) + 1e-9)

        if seasonality_strength > 1.2 or spike_ratio > 4.0:
            return "HIGH_SEASONAL"
        elif seasonality_strength > 0.7 or spike_ratio > 2.5:
            return "MODERATE"

    return "STABLE"


# ── Related queries ───────────────────────────────────────────────────────────

def get_related_queries(pytrends: TrendReq, seed: str) -> list[str]:
    """Lấy related queries thật từ Google Trends cho một seed keyword."""
    try:
        pytrends.build_payload([seed], geo=GEO, timeframe="today 12-m")
        related = pytrends.related_queries()
        time.sleep(random.uniform(4.0, 7.0))

        results = []
        data    = related.get(seed, {})
        for key in ["top", "rising"]:
            df = data.get(key)
            if df is not None and not df.empty:
                for q in df["query"].tolist():
                    wc = len(q.split())
                    if MIN_KEYWORD_WORDS <= wc <= MAX_KEYWORD_WORDS:
                        results.append(q.strip().lower())

        seen     = set()
        filtered = []
        for q in results:
            if q not in seen:
                seen.add(q)
                filtered.append(q)
            if len(filtered) >= MAX_RELATED_PER_SEED:
                break

        logger.info(f"  Related queries '{seed}': {len(filtered)} keywords")
        return filtered

    except Exception as e:
        logger.warning(f"  Không lấy được related queries cho '{seed}': {e}")
        return []


def build_keyword_list(pytrends: TrendReq, use_cache: bool = True) -> list[str]:
    """
    Xây dựng danh sách keyword hoàn chỉnh:
    1. MANUAL_KEYWORDS — luôn có
    2. Related queries từ SEED_KEYWORDS — keyword thật người dùng tìm
    3. Dedup toàn bộ
    Cache vào JSON để không gọi API lại.
    """
    CHECKPOINT_DIR.mkdir(parents=True, exist_ok=True)

    cached_related: dict[str, list[str]] = {}
    if use_cache and RELATED_CACHE.exists():
        try:
            with open(RELATED_CACHE, encoding="utf-8") as f:
                cached_related = json.load(f)
            logger.info(f"Loaded related queries cache: {len(cached_related)} seeds")
        except Exception:
            cached_related = {}

    all_related: list[str] = []
    cache_updated = False

    for seed in SEED_KEYWORDS:
        if seed in cached_related:
            logger.info(f"  Cache hit: '{seed}' ({len(cached_related[seed])} queries)")
            all_related.extend(cached_related[seed])
        else:
            logger.info(f"  Fetching related queries: '{seed}'")
            queries = get_related_queries(pytrends, seed)
            cached_related[seed] = queries
            all_related.extend(queries)
            cache_updated = True
            time.sleep(random.uniform(5.0, 8.0))

    if cache_updated:
        with open(RELATED_CACHE, "w", encoding="utf-8") as f:
            json.dump(cached_related, f, ensure_ascii=False, indent=2)
        logger.info(f"Related queries cache saved → {RELATED_CACHE}")

    combined = MANUAL_KEYWORDS.copy()
    seen     = set(k.lower() for k in combined)
    for kw in all_related:
        if kw not in seen:
            seen.add(kw)
            combined.append(kw)

    logger.info(f"Tổng keyword: {len(combined)} "
                f"({len(MANUAL_KEYWORDS)} manual + "
                f"{len(combined) - len(MANUAL_KEYWORDS)} related)")
    return combined


# ── Emerging trend detection ──────────────────────────────────────────────────

def detect_emerging_trend(df_kw: pd.DataFrame) -> dict:
    """So sánh 4 tuần gần nhất với baseline 6 tháng."""
    min_len = (EMERGING_TREND_BASELINE + EMERGING_TREND_WINDOW) * 7
    if len(df_kw) < min_len:
        return {"status": "⚪ Không đủ data", "growth_rate": None,
                "recent_avg": None, "baseline_avg": None}

    recent_days   = EMERGING_TREND_WINDOW * 7
    baseline_days = EMERGING_TREND_BASELINE * 7
    recent        = df_kw.tail(recent_days)["value"].mean()
    baseline      = df_kw.iloc[-(baseline_days + recent_days):-recent_days]["value"].mean()
    growth        = (recent - baseline) / (baseline + 1e-9)

    if growth > EMERGING_TREND_THRESHOLD:
        status = "🚀 Đang nổi lên"
    elif growth > -0.15:
        status = "➡️  Ổn định"
    else:
        status = "📉 Đang giảm"

    return {
        "status":       status,
        "growth_rate":  round(growth, 4),
        "recent_avg":   round(recent, 2),
        "baseline_avg": round(baseline, 2),
    }


# ── Window helpers ────────────────────────────────────────────────────────────

def date_windows(start: date, end: date, months: int, overlap_days: int) -> list[tuple]:
    """Sinh ra list (start, end) khoảng crawl có overlap chính xác."""
    windows = []
    cur = start
    while cur < end:
        m = cur.month + months
        y = cur.year + (m - 1) // 12
        m = (m - 1) % 12 + 1
        try:
            target_end = date(y, m, cur.day)
        except ValueError:
            target_end = date(y, m, 1) - timedelta(days=1)

        win_end = min(target_end, end)
        windows.append((cur, win_end))
        if win_end >= end:
            break
        cur = win_end - timedelta(days=overlap_days)
    return windows


def normalize_overlap(df_prev: pd.DataFrame, df_curr: pd.DataFrame) -> pd.DataFrame:
    """Chuẩn hoá df_curr về cùng scale với df_prev dựa vào vùng overlap."""
    overlap = df_prev.merge(df_curr, on="date", suffixes=("_prev", "_curr"))

    if overlap.empty or overlap["value_prev"].sum() == 0 or overlap["value_curr"].sum() == 0:
        return df_curr[df_curr["date"] > df_prev["date"].max()].copy()

    mask = (overlap["value_curr"] > 0) & (overlap["value_prev"] > 0)
    if mask.sum() == 0:
        return df_curr[df_curr["date"] > df_prev["date"].max()].copy()

    ratio  = (overlap.loc[mask, "value_prev"] / overlap.loc[mask, "value_curr"]).median()
    df_new = df_curr[df_curr["date"] > df_prev["date"].max()].copy()
    df_new["value"] = df_new["value"] * ratio
    return df_new


# ── Checkpoint helpers ────────────────────────────────────────────────────────

def checkpoint_path(kw: str) -> Path:
    return CHECKPOINT_DIR / f"{kw.replace(' ', '_').replace('/', '_')}.csv"


def load_checkpoint(kw: str) -> pd.DataFrame | None:
    path = checkpoint_path(kw)
    if path.exists():
        df = pd.read_csv(path, parse_dates=["date"])
        logger.info(f"  ✓ Checkpoint: {len(df)} rows, đến {df['date'].max().date()}")
        return df
    return None


def save_checkpoint(kw: str, df: pd.DataFrame) -> None:
    CHECKPOINT_DIR.mkdir(parents=True, exist_ok=True)
    df.to_csv(checkpoint_path(kw), index=False)


# ── Crawl 1 keyword ───────────────────────────────────────────────────────────

def crawl_keyword(pytrends: TrendReq, kw: str, resume: bool = True) -> pd.DataFrame:
    """Crawl toàn bộ lịch sử cho một keyword, hỗ trợ resume từ checkpoint."""
    windows = date_windows(START_DATE, END_DATE, WINDOW_MONTHS, OVERLAP_DAYS)

    existing_df       = None
    start_window_idx  = 0
    if resume:
        existing_df = load_checkpoint(kw)
        if existing_df is not None:
            last_date = existing_df["date"].max()
            for idx, (w_start, w_end) in enumerate(windows):
                if pd.Timestamp(w_end) > last_date - timedelta(days=OVERLAP_DAYS):
                    start_window_idx = max(0, idx - 1)
                    break
            else:
                logger.info(f"  ✓ Đã crawl đầy đủ")
                return existing_df

    remaining_windows = windows[start_window_idx:]
    logger.info(f"  {len(remaining_windows)}/{len(windows)} windows cần crawl")

    all_segments: list[pd.DataFrame] = []
    if existing_df is not None and start_window_idx > 0:
        cutoff   = pd.Timestamp(windows[start_window_idx][0])
        old_data = existing_df[existing_df["date"] < cutoff].copy()
        if not old_data.empty:
            all_segments.append(old_data)

    start_time = time.time()

    for i, (w_start, w_end) in enumerate(remaining_windows):
        abs_idx   = start_window_idx + i + 1
        timeframe = f"{w_start.strftime('%Y-%m-%d')} {w_end.strftime('%Y-%m-%d')}"
        elapsed   = time.time() - start_time
        eta_s     = (elapsed / max(i, 1)) * (len(remaining_windows) - i - 1) if i > 0 else 0
        eta_str   = f"ETA ~{int(eta_s//60)}m{int(eta_s%60)}s" if i > 0 else "ETA: ..."

        logger.info(f"  Window {abs_idx}/{len(windows)} | {timeframe} | "
                    f"{(i+1)/len(remaining_windows):.0%} | {eta_str}")

        df = None
        for attempt in range(3):
            try:
                pytrends.build_payload([kw], geo=GEO, timeframe=timeframe)
                df = pytrends.interest_over_time()
                break
            except Exception as e:
                wait_time = 30 * (attempt + 1)
                logger.warning(f"    Attempt {attempt+1} lỗi: {e}. Sleep {wait_time}s...")
                time.sleep(wait_time)
        else:
            logger.error(f"    Bỏ qua window {timeframe} do lỗi liên tiếp")
            continue

        if df is None or df.empty:
            logger.warning(f"    Không có data")
            continue

        if "isPartial" in df.columns:
            df = df.drop(columns=["isPartial"])

        df = df.reset_index()[["date", kw]].rename(columns={kw: "value"})
        df["date"] = pd.to_datetime(df["date"])
        all_segments.append(df)
        time.sleep(random.uniform(3.0, 6.0))

    if not all_segments:
        return pd.DataFrame()

    result = all_segments[0].copy()
    for seg in all_segments[1:]:
        normalized = normalize_overlap(result, seg)
        result     = pd.concat([result, normalized], ignore_index=True)

    result = (result
              .drop_duplicates(subset=["date"])
              .sort_values("date")
              .reset_index(drop=True))

    max_val = result["value"].max()
    if max_val > 0:
        result["value"] = (result["value"] / max_val) * 100
    result["value"] = result["value"].round(2)

    save_checkpoint(kw, result)
    logger.info(f"  → {len(result)} daily rows | checkpoint saved")
    return result


# ── Main ──────────────────────────────────────────────────────────────────────

def main(
    use_related_queries: bool = True,
    use_cache:           bool = True,
    resume:              bool = True,
):
    OUTPUT.parent.mkdir(exist_ok=True)
    CHECKPOINT_DIR.mkdir(parents=True, exist_ok=True)

    pytrends = TrendReq(hl="vi-VN", tz=420, timeout=(15, 45), retries=5, backoff_factor=2.5)

    # ── Bước 1: Xây dựng danh sách keyword ───────────────────────────────────
    logger.info("=== Bước 1: Xây dựng keyword list ===")
    if use_related_queries:
        keywords = build_keyword_list(pytrends, use_cache=use_cache)
    else:
        keywords = MANUAL_KEYWORDS.copy()
        logger.info(f"Manual keywords: {len(keywords)}")

    # ── Bước 2: Crawl từng keyword ────────────────────────────────────────────
    logger.info(f"\n=== Bước 2: Crawl {len(keywords)} keywords ===")
    all_frames: list[pd.DataFrame] = []
    trend_report: list[dict]       = []

    for idx, kw in enumerate(keywords):
        logger.info(f"\n[{idx+1}/{len(keywords)}] '{kw}'")

        df = crawl_keyword(pytrends, kw, resume=resume)
        if df.empty:
            logger.warning(f"  Không có data, bỏ qua")
            continue

        # Phân tích sau khi crawl
        trend_info = detect_emerging_trend(df)
        cluster    = assign_cluster(kw, df)
        attrs      = map_keyword_to_attributes(kw)

        logger.info(f"  Trend: {trend_info['status']} | Cluster: {cluster} | "
                    f"Attrs: {attrs}")

        # ── Export đầy đủ attrs cho train.py ─────────────────────────────────
        trend_report.append({
            "keyword":           kw,
            "cluster":           cluster,
            "trend_status":      trend_info["status"],
            "growth_rate":       trend_info["growth_rate"],
            "recent_avg":        trend_info["recent_avg"],
            "baseline_avg":      trend_info["baseline_avg"],
            "rows":              len(df),
            # Các cột attr_ — train.py đọc để suy ra use_log, back_to_school,...
            "attr_pattern":      attrs.get("PATTERN", ""),
            "attr_sub_category": attrs.get("SUB_CATEGORY", ""),
            "attr_category":     attrs.get("CATEGORY", ""),
            "attr_fit_type":     attrs.get("FIT_TYPE", ""),
            "attr_demographic":  attrs.get("TARGET_DEMOGRAPHIC", ""),
            "attr_material":     attrs.get("MATERIAL", ""),
        })

        df["keyword"] = kw
        all_frames.append(df)

        if idx < len(keywords) - 1:
            wait = random.uniform(6.0, 10.0)
            logger.info(f"  Chờ {wait:.1f}s...")
            time.sleep(wait)

    if not all_frames:
        logger.error("Không crawl được data nào")
        return

    # ── Bước 3: Merge với data cũ, dedup, lưu ────────────────────────────────
    logger.info("\n=== Bước 3: Merge và lưu ===")
    new_result             = pd.concat(all_frames, ignore_index=True)
    new_result["date"]     = pd.to_datetime(new_result["date"])

    if OUTPUT.exists():
        old_df     = pd.read_csv(OUTPUT, parse_dates=["date"])
        final_df   = pd.concat([old_df, new_result], ignore_index=True)
    else:
        final_df = new_result

    total_before = len(final_df)
    final_df = (final_df
                .drop_duplicates(subset=["date", "keyword"], keep="last")
                .sort_values(["keyword", "date"])
                .reset_index(drop=True))

    final_df.to_csv(OUTPUT, index=False)
    logger.info(f"Loại bỏ {total_before - len(final_df)} duplicates")
    logger.info(f"✅ {OUTPUT.resolve()} | {len(final_df)} rows | "
                f"{final_df['keyword'].nunique()} keywords")

    # ── Bước 4: Lưu trend_report.csv ─────────────────────────────────────────
    if trend_report:
        report_df = pd.DataFrame(trend_report)
        report_df.to_csv(TREND_REPORT_PATH, index=False)
        logger.info(f"✅ {TREND_REPORT_PATH.resolve()}")

        logger.info(f"\n=== Trend Report ===")
        cols = ["keyword", "cluster", "trend_status", "growth_rate",
                "rows", "attr_sub_category", "attr_pattern"]
        logger.info(f"\n{report_df[cols].to_string(index=False)}")

        emerging = report_df[report_df["trend_status"].str.contains("nổi lên", na=False)]
        if not emerging.empty:
            logger.info(f"\n🚀 Đang nổi lên ({len(emerging)}):")
            for _, row in emerging.iterrows():
                logger.info(f"   {row['keyword']} | +{row['growth_rate']:.1%} | {row['cluster']}")


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--no-related", action="store_true")
    parser.add_argument("--no-cache",   action="store_true")
    parser.add_argument("--no-resume",  action="store_true")
    args = parser.parse_args()

    main(
        use_related_queries=not args.no_related,
        use_cache=not args.no_cache,
        resume=not args.no_resume,
    )
import subprocess
subprocess.run(["python", "merge_keywords.py",
                "--input", "google_trends_raw.csv",
                "--output", "google_trends_merged.csv",
                "--mode", "sum"])