"""
merge_keywords.py
-----------------
Tự động merge/drop keyword trùng lặp sau mỗi lần crawl Google Trends.

Cách dùng:
    python merge_keywords.py --input google_trends_raw.csv --output google_trends_merged.csv

Tuỳ chọn:
    --input   : file CSV đầu vào  (cột: date, value, keyword)
    --output  : file CSV đầu ra   (cùng cấu trúc, keyword đã được merge)
    --log     : in chi tiết từng thao tác merge/drop (mặc định: True)
    --mode    : 'sum' | 'max' | 'mean'  — cách gộp value khi merge (mặc định: sum)
"""

import argparse
import pandas as pd
import sys
from datetime import datetime

# ─────────────────────────────────────────────
# MERGE MAP
# key   = keyword bị bỏ / gộp vào
# value = keyword đại diện (None = drop hoàn toàn)
#
# Cơ sở quyết định:
#   MERGE  → correlation ≥ 0.4 HOẶC volume chênh > 10× (keyword nhỏ gộp vào lớn)
#   DROP   → volume mean < 1 VÀ correlation < 0.15 với mọi keyword liên quan
# ─────────────────────────────────────────────
MERGE_MAP = {

    # ── Bản không dấu → bản có dấu ──────────────────────────────────────
    "ao khoac jean":            "áo khoác jean",          # corr ~0, vol 0.53 vs 3.78 → gộp
    "ao so mi nam":             "áo sơ mi nam",           # corr -0.04, vol 1.06 vs 35 → gộp
    "giay sneaker":             None,                     # corr 0.1, vol 0.88, noise thuần → drop
    "quan jean nu":             None,                     # corr 0.1, vol 0.53, noise → drop
    "quan short nam":           "quần short nam kaki",    # đại diện generic nhất trong nhóm
    "tui xach nu":              None,                     # corr -0.055, vol 0.61 → drop
    "ao phong oversize":        "áo thun nam",            # cùng category, gộp vào generic

    # ── Modifier mơ hồ (đẹp / xinh / ngắn) ─────────────────────────────
    "váy đẹp":                  "váy nữ",                 # corr 0.779 → merge
    "đầm nữ đẹp":               None,                     # corr 0.13 với đầm nữ, vol 0.78 → drop
    "áo đầm nữ":                None,                     # corr -0.015 với đầm nữ, vol 0.72 → drop
    "áo sơ mi đẹp":             "áo sơ mi",               # corr 0.794 → merge
    "mẫu áo sơ mi nam":         None,                     # corr -0.076, vol 0.7 → drop
    "áo sơ mi nam đẹp":         "áo sơ mi nam",           # modifier mơ hồ
    "áo thun nam đẹp":          None,                     # corr 0.237 vs vol 1.26 → drop
    "quần short nam đẹp":       None,                     # corr 0.054, vol 0.37 → drop
    "quần short nam ngắn":      None,                     # "ngắn" là mặc định của short → drop
    "quần jean nữ đẹp":         None,                     # corr 0.106, vol 0.79 → drop
    "sandal nữ đẹp":            None,                     # corr ~0, vol 0.44 → drop
    "giày sandal nữ đẹp":       None,                     # corr ~0, vol 0.44 → drop
    "dép sandal nữ đẹp":        None,                     # corr ~0, vol 0.30 → drop
    "túi xách nữ đẹp":          None,                     # corr -0.055, vol 0.80 → drop
    "váy nữ đẹp":               "váy nữ",                 # modifier mơ hồ

    # ── Shop / Size (navigational / utility) ────────────────────────────
    "shop áo thun nam":         None,                     # corr 0.082, utility query → drop
    "size áo thun nam":         None,                     # corr 0.048 → drop
    "shop quần jean nữ":        None,                     # corr 0.056 → drop
    "size quần jean nữ":        None,                     # corr -0.037 → drop
    "size áo khoác":            None,                     # utility query → drop
    "size áo sơ mi nam":        None,                     # utility query → drop
    "shop giày sneaker":        "giày sneaker nam",       # navigational → gộp vào product
    "shop đầm nữ":              None,                     # corr thấp, vol 0.71 → drop

    # ── Trùng brand / sản phẩm ──────────────────────────────────────────
    "sơ mi trắng nam":          "áo sơ mi nam trắng",     # corr 0.399, cùng sản phẩm → merge
    "túi pedro nữ":             "túi xách pedro",         # corr 0.710 → merge
    "áo thun nam polo":         "áo polo nam",            # cùng sản phẩm, vol 0.7 → merge
    "áo thun polo":             "áo polo",                # corr 0.554 → merge
    "áo thun nam có cổ":        "áo polo nam",            # cùng sản phẩm (polo = có cổ)
    "sneaker nam":              "giày sneaker nam",       # corr 0.831 → merge

    # ── Use-case trùng lặp ───────────────────────────────────────────────
    "giày sandal nữ đi học":    "sandal nữ đi học",       # corr 0.028, gộp thành 1 (cùng intent, cả 2 nhỏ)
    "dép sandal nữ":            "dép sandal",             # corr 0.143, vol 0.9 vs 8.3 → merge vào lớn

    # ── Modifier generic thừa ───────────────────────────────────────────
    "phối đồ với quần short nam": None,                   # editorial/inspiration, vol 0.71 → drop
    "áo khoác đẹp":             "áo khoác nữ",            # modifier mơ hồ → merge vào generic nữ
    "đầm nữ trung niên":        None,                     # segment hẹp, vol 0.70, corr ~0 → drop
                                                          # (có thể đổi thành keep nếu có chiến dịch riêng)
}

# ── Keywords cần giữ nguyên (không merge dù tên gần) ────────────────────
# Ghi chú để tránh thêm nhầm vào MERGE_MAP
KEEP_SEPARATE = [
    "váy dài", "váy ngắn",           # khác sản phẩm (độ dài)
    "chân váy", "chân váy dài",      # sản phẩm khác (skirt ≠ dress)
    "quần jean nữ ngắn", "quần jean nữ ống rộng",  # cut khác nhau
    "quần short nam jean", "quần short nam kaki",  # chất liệu khác
    "quần short nam đi biển", "quần short nam cao cấp",
    "sandal nữ đế cao", "sandal nữ đi học", "sandal nữ bitis",
    "giày sneaker adidas", "giày sneaker nike",
    "áo khoác bomber", "áo khoác da", "áo khoác len",
    "áo khoác lông", "áo khoác dạ", "áo khoác gió",
    "áo khoác uniqlo", "áo khoác adidas", "áo khoác đồng phục",
    "áo sơ mi nam trắng", "áo sơ mi nam tay ngắn",
    "áo sơ mi nam cao cấp", "áo sơ mi nam việt tiến",
    "áo sơ mi sọc nam", "áo sơ mi đen", "áo sơ mi trắng",
    "túi charles & keith", "túi xách charles & keith",  # giữ riêng 6 tháng quan sát
    "pedro túi",                     # branded search khác intent
    "váy xinh",                      # corr 0.10 với váy nữ → pattern riêng
    "váy ngủ",                       # sản phẩm khác
    "váy trắng",                     # màu cụ thể
    "vay hoa",                       # pattern riêng
]


def validate_merge_map(merge_map: dict, keep_separate: list) -> None:
    """Kiểm tra MERGE_MAP không conflict với KEEP_SEPARATE."""
    conflicts = [k for k in merge_map if k in keep_separate]
    if conflicts:
        print(f"[WARN] Conflict: keyword vừa trong MERGE_MAP vừa trong KEEP_SEPARATE: {conflicts}")
    targets = [v for v in merge_map.values() if v is not None]
    target_conflicts = [t for t in targets if t in merge_map]
    if target_conflicts:
        print(f"[WARN] Target keyword cũng là source trong MERGE_MAP (chain merge): {target_conflicts}")


def merge_trends(
    df: pd.DataFrame,
    merge_map: dict,
    mode: str = "sum",
    log: bool = True,
) -> pd.DataFrame:
    """
    Thực hiện merge/drop theo MERGE_MAP.

    Parameters
    ----------
    df       : DataFrame với cột [date, value, keyword]
    merge_map: dict keyword_bỏ → keyword_đại_diện (None = drop)
    mode     : cách gộp value — 'sum', 'max', 'mean'
    log      : in log chi tiết

    Returns
    -------
    DataFrame đã merge, sort theo (keyword, date)
    """
    agg_fn = {"sum": "sum", "max": "max", "mean": "mean"}[mode]

    original_kws  = set(df["keyword"].unique())
    map_sources   = set(merge_map.keys())
    unknown       = map_sources - original_kws

    if unknown and log:
        print(f"[INFO] Keyword trong MERGE_MAP nhưng không có trong data (bỏ qua): {sorted(unknown)}")

    total_rows_before = len(df)
    dropped_kws, merged_kws = [], []

    for src, tgt in merge_map.items():
        if src not in original_kws:
            continue
        if tgt is None:
            df = df[df["keyword"] != src]
            dropped_kws.append(src)
            if log:
                n = (df["keyword"] == src).sum()  # after drop = 0, so count before
                print(f"  [DROP]  '{src}'")
        else:
            if tgt not in original_kws and tgt not in [merge_map.get(k) for k in merge_map]:
                if log:
                    print(f"  [WARN]  Target '{tgt}' không tồn tại trong data — bỏ qua merge '{src}'")
                continue
            # Đổi tên keyword source → target, sau đó groupby để gộp value
            df.loc[df["keyword"] == src, "keyword"] = tgt
            merged_kws.append((src, tgt))
            if log:
                print(f"  [MERGE] '{src}' → '{tgt}'  (mode={mode})")

    # Sau khi đổi tên, groupby để gộp các row cùng (date, keyword)
    df = (
        df.groupby(["date", "keyword"], as_index=False)["value"]
        .agg(agg_fn)
    )

    total_rows_after = len(df)
    final_kws = set(df["keyword"].unique())

    if log:
        print(f"\n{'─'*50}")
        print(f"  Keywords trước : {len(original_kws):>4}")
        print(f"  Keywords sau   : {len(final_kws):>4}  (−{len(original_kws)-len(final_kws)})")
        print(f"  Rows trước     : {total_rows_before:>7,}")
        print(f"  Rows sau       : {total_rows_after:>7,}")
        print(f"  Đã DROP  : {len(dropped_kws)} keywords")
        print(f"  Đã MERGE : {len(merged_kws)} cặp")
        print(f"{'─'*50}")

    return df.sort_values(["keyword", "date"]).reset_index(drop=True)


def main():
    parser = argparse.ArgumentParser(description="Auto-merge Google Trends keywords post-crawl")
    parser.add_argument("--input",  default="google_trends_raw.csv",    help="File CSV đầu vào")
    parser.add_argument("--output", default="google_trends_merged.csv", help="File CSV đầu ra")
    parser.add_argument("--mode",   default="sum", choices=["sum","max","mean"],
                        help="Cách gộp value khi merge (default: sum)")
    parser.add_argument("--no-log", action="store_true", help="Tắt log chi tiết")
    args = parser.parse_args()

    log = not args.no_log
    ts  = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    print(f"\n{'═'*50}")
    print(f"  merge_keywords.py  [{ts}]")
    print(f"  input  : {args.input}")
    print(f"  output : {args.output}")
    print(f"  mode   : {args.mode}")
    print(f"{'═'*50}\n")

    # 1. Đọc data
    try:
        df = pd.read_csv(args.input, parse_dates=["date"])
    except FileNotFoundError:
        print(f"[ERROR] Không tìm thấy file: {args.input}")
        sys.exit(1)

    required_cols = {"date", "value", "keyword"}
    if not required_cols.issubset(df.columns):
        print(f"[ERROR] File thiếu cột. Cần: {required_cols}, có: {set(df.columns)}")
        sys.exit(1)

    # 2. Validate config
    validate_merge_map(MERGE_MAP, KEEP_SEPARATE)

    # 3. Merge
    df_merged = merge_trends(df, MERGE_MAP, mode=args.mode, log=log)

    # 4. Ghi output
    df_merged.to_csv(args.output, index=False)
    print(f"\n[OK] Đã lưu: {args.output}  ({len(df_merged):,} rows, {df_merged['keyword'].nunique()} keywords)\n")


if __name__ == "__main__":
    main()