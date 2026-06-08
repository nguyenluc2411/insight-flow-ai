"""
train.py
--------
Đọc CSV từ crawl.py và trend_report.csv, tự động phân giải cấu hình hyperparameter
và hiệu ứng mùa vụ dựa trên Attributes (Catalog-driven), sau đó train Prophet.
"""

import pickle
import logging
import numpy as np
import pandas as pd
from datetime import date, timedelta
from pathlib import Path
from prophet import Prophet

# ── Config ────────────────────────────────────────────────────────────────────

INPUT_CSV      = Path("./output/google_trends_raw.csv")
TREND_REPORT   = Path("./output/trend_report.csv")
OUTPUT_DIR     = Path("./output")
MODEL_DIR      = Path("./models")
FORECAST_DAYS  = 90

# Bộ khung cấu hình Hyperparameter nền tảng dựa theo Cluster (thay vì tên keyword)
CLUSTER_CONFIGS = {
    "HIGH_SEASONAL": {
        "changepoint_prior_scale": 0.15,
        "seasonality_prior_scale": 15.0,
        "holidays_prior_scale":    8.0,
        "n_changepoints":          35,
    },
    "MODERATE": {
        "changepoint_prior_scale": 0.08,
        "seasonality_prior_scale": 10.0,
        "holidays_prior_scale":    5.0,
        "n_changepoints":          25,
    },
    "STABLE": {
        "changepoint_prior_scale": 0.05,
        "seasonality_prior_scale": 10.0,
        "holidays_prior_scale":    5.0,
        "n_changepoints":          25,
    }
}

DEFAULT_CONFIG = CLUSTER_CONFIGS["STABLE"]

# ── Logging ───────────────────────────────────────────────────────────────────

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(message)s",
    datefmt="%Y-%m-%dT%H:%M:%S",
)
logger = logging.getLogger(__name__)

OUTPUT_DIR.mkdir(exist_ok=True)
MODEL_DIR.mkdir(exist_ok=True)

# ── Holidays ──────────────────────────────────────────────────────────────────

def get_vietnam_holidays(start_year: int = 2019, end_year: int = 2028) -> pd.DataFrame:
    _tet = {
        2019: date(2019, 2,  5),  2020: date(2020, 1, 25),
        2021: date(2021, 2, 12),  2022: date(2022, 2,  1),
        2023: date(2023, 1, 22),  2024: date(2024, 2, 10),
        2025: date(2025, 1, 29),  2026: date(2026, 2, 17),
        2027: date(2027, 2,  6),  2028: date(2028, 1, 26),
    }
    rows = []

    for year in range(start_year, end_year + 1):
        tet = _tet.get(year, date(year, 2, 5))
        rows += [
            {"holiday": "tet",           "ds": pd.Timestamp(tet),                 "lower_window": -14, "upper_window": 7},
            {"holiday": "phu_nu_8_3",    "ds": pd.Timestamp(date(year, 3,  8)),   "lower_window":  -3, "upper_window": 1},
            {"holiday": "gp_30_4",       "ds": pd.Timestamp(date(year, 4, 30)),   "lower_window":  -2, "upper_window": 2},
            {"holiday": "lao_dong_1_5",  "ds": pd.Timestamp(date(year, 5,  1)),   "lower_window":  -1, "upper_window": 1},
            {"holiday": "quoc_khanh",    "ds": pd.Timestamp(date(year, 9,  2)),   "lower_window":  -1, "upper_window": 2},
            {"holiday": "pn_vn_20_10",   "ds": pd.Timestamp(date(year, 10, 20)),  "lower_window":  -3, "upper_window": 1},
            {"holiday": "singles_11_11", "ds": pd.Timestamp(date(year, 11, 11)),  "lower_window":  -5, "upper_window": 1},
            {"holiday": "giang_sinh",    "ds": pd.Timestamp(date(year, 12, 25)),  "lower_window":  -5, "upper_window": 1},
            {"holiday": "tet_dl_1_1",    "ds": pd.Timestamp(date(year,  1,  1)),  "lower_window":  -2, "upper_window": 1},
        ]

        # Black Friday
        nov1 = date(year, 11, 1)
        bf   = nov1 + timedelta(days=(4 - nov1.weekday()) % 7) + timedelta(weeks=3)
        rows.append({"holiday": "black_friday", "ds": pd.Timestamp(bf), "lower_window": -3, "upper_window": 2})

        # Sàn TMĐT song ngày sale
        double_day_sales = [
            ("shopee_5_5", 5, 5, -2, 1), ("shopee_6_6", 6, 6, -3, 1), ("lazada_6_6", 6, 6, -3, 1),
            ("shopee_7_7", 7, 7, -3, 1), ("lazada_7_7", 7, 7, -3, 1), ("shopee_8_8", 8, 8, -3, 1),
            ("lazada_8_8", 8, 8, -3, 1), ("shopee_9_9", 9, 9, -3, 1), ("lazada_9_9", 9, 9, -3, 1),
            ("shopee_10_10", 10, 10, -3, 1), ("lazada_10_10", 10, 10, -3, 1),
        ]
        for name, month, day, lw, uw in double_day_sales:
            rows.append({"holiday": name, "ds": pd.Timestamp(date(year, month, day)), "lower_window": lw, "upper_window": uw})

        rows.append({"holiday": "mega_sale_12_12", "ds": pd.Timestamp(date(year, 12, 12)), "lower_window": -5, "upper_window": 1})
        rows.append({"holiday": "pre_tet_sale", "ds": pd.Timestamp(tet - timedelta(days=14)), "lower_window": -3, "upper_window": 5})
        rows.append({"holiday": "mid_year_sale", "ds": pd.Timestamp(date(year, 6, 25)), "lower_window": -5, "upper_window": 2})

    df = pd.DataFrame(rows)
    df["ds"] = pd.to_datetime(df["ds"])
    return df.drop_duplicates(subset=["holiday", "ds"]).reset_index(drop=True)

VN_HOLIDAYS = get_vietnam_holidays()

# ── Dynamic Configuration Resolver ────────────────────────────────────────────

def resolve_keyword_flags(meta: dict) -> dict:
    """
    Tách hoàn toàn logic cứng dựa trên text, phân giải hành vi dữ liệu bằng Catalog Attributes.
    """
    keyword      = meta.get("keyword", "").lower()
    pattern      = meta.get("attr_pattern", "")
    sub_category = meta.get("attr_sub_category", "")
    category     = meta.get("attr_category", "")
    cluster      = meta.get("cluster", "STABLE")

    # 1. Log Transform: Áp dụng với nhóm biến động mạnh (mùa vụ cao)
    use_log = (
        cluster == "HIGH_SEASONAL" or
        pattern in {"Floral"} or
        sub_category in {"Skirt", "Dress"} or
        (category == "Footwear" and "sandal" in keyword)
    )

    # 2. Back to school effect: Áo sơ mi (Shirt), Giày dép (Footwear), Quần short (Shorts)
    back_to_school = (
        sub_category in {"Shirt", "Shorts"} or 
        category == "Footwear"
    )

    # 3. Explicit Changepoints (Bảo lưu logic proxy từ file vấn đề nếu cần can thiệp COVID)
    # Nếu data dài (> 3 năm tương đương khoảng > 1000 daily rows), dùng để tinh chỉnh changepoints
    history_rows = meta.get("rows", 0)
    explicit_changepoints = (
        history_rows > 1000 and 
        sub_category in {"Shirt", "Jeans", "Trousers", "Shorts"}
    )

    return {
        "use_log": use_log,
        "back_to_school": back_to_school,
        "explicit_changepoints": explicit_changepoints
    }

# ── Prophet Factory ───────────────────────────────────────────────────────────

def build_model(meta: dict, flags: dict) -> Prophet:
    """Khởi tạo Prophet với bộ tham số phân giải động từ metadata."""
    cluster = meta.get("cluster", "STABLE")
    cfg = CLUSTER_CONFIGS.get(cluster, DEFAULT_CONFIG)

    # Tùy biến sâu: nếu dùng explicit changepoints thì có thể hạ bớt changepoint_prior_scale tránh overfit
    if flags["explicit_changepoints"]:
        cfg["changepoint_prior_scale"] = min(cfg["changepoint_prior_scale"], 0.08)

    m = Prophet(
        holidays=VN_HOLIDAYS,
        yearly_seasonality=True,
        weekly_seasonality=True,
        daily_seasonality=False,
        interval_width=0.80,
        mcmc_samples=0,
        **cfg
    )

    # Thêm chu kỳ tháng cố định cho thời trang
    m.add_seasonality(name="monthly", period=30.5, fourier_order=3)

    # Thêm điều kiện Back-To-School động
    if flags["back_to_school"]:
        m.add_seasonality(
            name="back_to_school",
            period=365.25,
            fourier_order=3,
            condition_name="is_aug_sep"
        )

    return m

def add_condition_cols(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()
    df["is_aug_sep"] = df["ds"].dt.month.isin([8, 9]).astype(float)
    return df

def train_and_forecast(keyword: str, df_kw: pd.DataFrame, meta: dict) -> dict | None:
    df = df_kw[["date", "value"]].copy()
    df.columns = ["ds", "y"]
    df["ds"] = pd.to_datetime(df["ds"]).dt.normalize()
    df["y"]  = df["y"].astype(float)
    df = df.dropna().groupby("ds", as_index=False)["y"].mean()
    df = df.sort_values("ds").reset_index(drop=True)

    if len(df) < 180:
        logger.warning(f"  Không đủ data cho '{keyword}' ({len(df)} rows), bỏ qua")
        return None

    # Phân giải Flags & Khởi tạo Cấu hình
    flags = resolve_keyword_flags(meta)
    logger.info(f"  Training '{keyword}' ({len(df)} rows) | Cluster: {meta.get('cluster')} | Flags: {flags}")

    # Thực thi Log Transform nếu flags yêu cầu
    if flags["use_log"]:
        # Cộng 1 tránh log(0)
        df["y"] = np.log1p(df["y"])

    df = add_condition_cols(df)
    model = build_model(meta, flags)
    model.fit(df)

    # Lưu Model
    model_path = MODEL_DIR / f"prophet_{keyword.replace(' ', '_')}.pkl"
    with open(model_path, "wb") as f:
        pickle.dump(model, f)

    # Dự báo tương lai
    future = model.make_future_dataframe(periods=FORECAST_DAYS, freq="D", include_history=True)
    future = add_condition_cols(future)
    forecast = model.predict(future)

    # Đảo ngược Log Transform nếu đã áp dụng trước đó
    if flags["use_log"]:
        forecast[["yhat", "yhat_lower", "yhat_upper"]] = np.expm1(forecast[["yhat", "yhat_lower", "yhat_upper"]])
        df["y"] = np.expm1(df["y"])

    cols = ["ds", "yhat", "yhat_lower", "yhat_upper", "trend"]
    for component in ["yearly", "weekly", "monthly", "back_to_school"]:
        if component in forecast.columns:
            cols.append(component)

    fc = forecast[cols].copy()
    fc[["yhat", "yhat_lower", "yhat_upper"]] = fc[["yhat", "yhat_lower", "yhat_upper"]].clip(0, 100)

    # Đánh giá Metrics chất lượng mô hình
    merged = df[["ds", "y"]].merge(fc[["ds", "yhat"]], on="ds", how="inner")
    mae, rmse = None, None
    if len(merged) > 0:
        actual, predicted = merged["y"].values, merged["yhat"].values
        mae  = float(np.mean(np.abs(actual - predicted)))
        rmse = float(np.sqrt(np.mean((actual - predicted) ** 2)))
        logger.info(f"  MAE={mae:.2f} | RMSE={rmse:.2f}")

    return {
        "keyword": keyword,
        "mae": mae,
        "rmse": rmse,
        "history_rows": len(df),
        "forecast": fc,
        "config_type": f"Cluster_{meta.get('cluster')}"
    }

# ── Main Execution ────────────────────────────────────────────────────────────

def main():
    if not INPUT_CSV.exists():
        logger.error(f"Không tìm thấy {INPUT_CSV} — chạy crawl.py trước")
        return

    # Load file mapping metadata từ bước crawl
    cluster_map = {}
    if TREND_REPORT.exists():
        report_df = pd.read_csv(TREND_REPORT)
        # Điền chuỗi rỗng cho các trường thuộc tính bị khuyết để tránh dính NaN float
        report_df = report_df.fillna("")
        cluster_map = report_df.set_index("keyword").to_dict(orient="index")
        logger.info(f"Loaded {len(cluster_map)} keyword metadata từ trend_report.csv")
    else:
        logger.warning(f"Không tìm thấy {TREND_REPORT}, hệ thống sẽ fallback về cấu hình STABLE mặc định.")

    logger.info(f"Load raw trends data từ {INPUT_CSV}")
    trends_df = pd.read_csv(INPUT_CSV, parse_dates=["date"])
    keywords  = trends_df["keyword"].unique().tolist()

    summary_rows = []

    for kw in keywords:
        logger.info(f"\n{'='*60}")
        logger.info(f"Keyword: '{kw}'")
        
        df_kw  = trends_df[trends_df["keyword"] == kw]
        # Lấy bản ghi metadata từ map, hoặc tạo dict rỗng nếu keyword mới chưa cập nhật vào report
        meta_row = cluster_map.get(kw, {"keyword": kw, "cluster": "STABLE"})
        
        result = train_and_forecast(kw, df_kw, meta_row)
        if result is None:
            continue

        fc_path = OUTPUT_DIR / f"forecast_{kw.replace(' ', '_')}.csv"
        result["forecast"].to_csv(fc_path, index=False)

        summary_rows.append({
            "keyword":       kw,
            "config":        result["config_type"],
            "history_rows":  result["history_rows"],
            "mae":           round(result["mae"], 2) if result["mae"] is not None else None,
            "rmse":          round(result["rmse"], 2) if result["rmse"] is not None else None,
            "forecast_file": str(fc_path),
        })

    summary = pd.DataFrame(summary_rows)
    summary_path = OUTPUT_DIR / "training_summary.csv"
    summary.to_csv(summary_path, index=False)

    logger.info(f"\n{'='*60}\n=== KẾT QUẢ TRAIN ===\n\n{summary.to_string(index=False)}")

if __name__ == "__main__":
    main()