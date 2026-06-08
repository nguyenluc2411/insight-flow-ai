"""
detect_retrain.py
-----------------
So sánh distribution cũ vs mới sau mỗi lần merge,
tự động output danh sách keyword cần retrain theo priority.

Cách dùng:
    # Lần đầu: lưu snapshot từ file hiện tại
    python detect_retrain.py --snapshot --data google_trends_merged.csv

    # Sau mỗi lần crawl + merge: so sánh với snapshot
    python detect_retrain.py --data google_trends_merged.csv --prev snapshot.parquet

    # Với RMSE từ train results (optional, dùng để tính expected RMSE drift)
    python detect_retrain.py --data google_trends_merged.csv --prev snapshot.parquet --rmse train_results.csv

Output:
    retrain_priority.csv   — danh sách keyword + mức độ ưu tiên + lý do
    snapshot.parquet       — snapshot mới (ghi đè, dùng cho lần sau)
"""

import argparse
import json
import sys
from datetime import datetime
from pathlib import Path

import numpy as np
import pandas as pd
from scipy import stats


# ─────────────────────────────────────────────────────────────────
# THRESHOLDS — có thể điều chỉnh tuỳ tolerance của model
# ─────────────────────────────────────────────────────────────────
THRESHOLDS = {
    # Δ mean (%) để trigger mỗi level
    "must_mean_pct":    40,   # series shift quá lớn
    "should_mean_pct":  10,

    # Δ std (%) — đo amplitude / seasonality thay đổi
    "must_std_pct":     80,
    "should_std_pct":   40,

    # Correlation giữa old vs new series (window 90 ngày gần nhất)
    "ok_corr_min":      0.90,  # corr >= 0.90 → coi là ổn
    "should_corr_min":  0.70,  # corr 0.70–0.90 → nên retrain

    # KS test p-value — phân phối đã thay đổi đáng kể
    "ks_must_pval":     0.01,
    "ks_should_pval":   0.05,

    # Volume tối thiểu để đáng model (mean value)
    "min_volume":       0.5,
}


# ─────────────────────────────────────────────────────────────────
# HELPERS
# ─────────────────────────────────────────────────────────────────

def pct_change(old: float, new: float) -> float:
    if old == 0:
        return 0.0
    return (new - old) / abs(old) * 100


def compute_stats(series: pd.Series) -> dict:
    s = series.dropna()
    if len(s) == 0:
        return dict(mean=0, std=0, median=0, p95=0, pct_zero=100, n=0)
    return dict(
        mean   = float(s.mean()),
        std    = float(s.std()),
        median = float(s.median()),
        p95    = float(s.quantile(0.95)),
        pct_zero = float((s == 0).mean() * 100),
        n      = int(len(s)),
    )


def tail_corr(old: pd.Series, new: pd.Series, days: int = 90) -> float | None:
    """Correlation trên window 90 ngày gần nhất (overlap)."""
    if len(old) == 0 or len(new) == 0:
        return None
    # Align by index (date)
    combined = pd.concat([old.rename("old"), new.rename("new")], axis=1).dropna()
    if len(combined) < 14:
        return None
    tail = combined.tail(days)
    c = tail["old"].corr(tail["new"])
    return float(c) if not np.isnan(c) else None


def ks_test(old: pd.Series, new: pd.Series):
    """KS test so sánh phân phối."""
    o = old.dropna().values
    n = new.dropna().values
    if len(o) < 10 or len(n) < 10:
        return None, None
    stat, pval = stats.ks_2samp(o, n)
    return float(stat), float(pval)


def decide(
    d_mean: float,
    d_std: float,
    corr: float | None,
    ks_pval: float | None,
    new_mean: float,
    is_new: bool,
    th: dict,
) -> tuple[str, list[str]]:
    """
    Trả về (decision, [reasons]).
    decision: 'must' | 'should' | 'ok' | 'skip'
    """
    reasons = []

    if new_mean < th["min_volume"]:
        return "skip", ["Volume quá thấp để model"]

    if is_new:
        return "must", ["Keyword mới — chưa có model"]

    abs_dm = abs(d_mean)
    abs_ds = abs(d_std)

    # MUST conditions
    if abs_dm >= th["must_mean_pct"]:
        reasons.append(f"Δ mean {d_mean:+.1f}%")
    if abs_ds >= th["must_std_pct"]:
        reasons.append(f"Δ std {d_std:+.1f}%")
    if ks_pval is not None and ks_pval < th["ks_must_pval"]:
        reasons.append(f"KS p={ks_pval:.4f} (phân phối đổi mạnh)")
    if reasons:
        return "must", reasons

    # SHOULD conditions
    if abs_dm >= th["should_mean_pct"]:
        reasons.append(f"Δ mean {d_mean:+.1f}%")
    if abs_ds >= th["should_std_pct"]:
        reasons.append(f"Δ std {d_std:+.1f}%")
    if corr is not None and corr < th["should_corr_min"]:
        reasons.append(f"Corr {corr:.3f} (trend drift)")
    if ks_pval is not None and ks_pval < th["ks_should_pval"]:
        reasons.append(f"KS p={ks_pval:.4f}")
    if reasons:
        return "should", reasons

    # OK
    ok_reasons = []
    if corr is not None:
        ok_reasons.append(f"Corr {corr:.3f}")
    ok_reasons.append(f"Δ mean {d_mean:+.1f}%  Δ std {d_std:+.1f}%")
    return "ok", ok_reasons


# ─────────────────────────────────────────────────────────────────
# SNAPSHOT
# ─────────────────────────────────────────────────────────────────

def save_snapshot(df: pd.DataFrame, path: str) -> None:
    """Lưu stats per keyword vào parquet (nhẹ hơn full CSV)."""
    records = []
    for kw, grp in df.groupby("keyword"):
        s = compute_stats(grp["value"])
        s["keyword"] = kw
        s["snapshot_date"] = datetime.now().isoformat()
        # Lưu full series để tính corr sau
        s["_series_json"] = grp.set_index("date")["value"].to_json(date_format="iso")
        records.append(s)
    snap = pd.DataFrame(records)
    snap.to_parquet(path, index=False)
    print(f"[OK] Snapshot lưu tại: {path}  ({len(snap)} keywords)")


def load_snapshot(path: str) -> pd.DataFrame:
    return pd.read_parquet(path)


# ─────────────────────────────────────────────────────────────────
# MAIN COMPARISON
# ─────────────────────────────────────────────────────────────────

def detect_retrain(
    df_new: pd.DataFrame,
    snap: pd.DataFrame,
    rmse_map: dict,
    th: dict,
    log: bool = True,
) -> pd.DataFrame:
    """
    So sánh df_new với snapshot, trả về DataFrame với cột:
    keyword, decision, priority_score, reasons, old_mean, new_mean,
    d_mean_pct, old_std, new_std, d_std_pct, corr_90d, ks_pval, old_rmse
    """
    old_kws  = set(snap["keyword"].tolist())
    new_kws  = set(df_new["keyword"].unique())
    dropped  = old_kws - new_kws
    added    = new_kws - old_kws
    common   = old_kws & new_kws

    if log:
        print(f"\n  Keywords snapshot  : {len(old_kws)}")
        print(f"  Keywords hiện tại  : {len(new_kws)}")
        print(f"  Mới thêm  (+)      : {len(added)}  {sorted(added) if added else ''}")
        print(f"  Đã xóa   (-)       : {len(dropped)}  {sorted(dropped) if dropped else ''}")
        print(f"  So sánh            : {len(common)}\n")

    rows = []

    # --- Dropped keywords ---
    for kw in sorted(dropped):
        rows.append(dict(
            keyword=kw, decision="dropped", priority_score=0,
            reasons="Đã xóa khỏi dataset — remove khỏi inference pipeline",
            old_mean=snap.loc[snap.keyword==kw,"mean"].values[0],
            new_mean=0, d_mean_pct=None, old_std=None,
            new_std=None, d_std_pct=None, corr_90d=None,
            ks_pval=None, old_rmse=rmse_map.get(kw),
        ))

    # --- Common keywords ---
    snap_idx = snap.set_index("keyword")
    for kw in sorted(common):
        grp_new = df_new[df_new["keyword"] == kw].set_index("date")["value"].sort_index()
        old_row  = snap_idx.loc[kw]

        # Reconstruct old series from JSON
        try:
            old_series = pd.read_json(old_row["_series_json"], typ="series")
            old_series.index = pd.to_datetime(old_series.index)
        except Exception:
            old_series = pd.Series(dtype=float)

        s_new = compute_stats(grp_new)
        s_old = dict(
            mean    = float(old_row["mean"]),
            std     = float(old_row["std"]),
            pct_zero= float(old_row["pct_zero"]),
        )

        d_mean = pct_change(s_old["mean"], s_new["mean"])
        d_std  = pct_change(s_old["std"],  s_new["std"])

        corr   = tail_corr(old_series, grp_new, days=90)
        ks_stat, ks_pval = ks_test(old_series, grp_new)

        dec, reasons = decide(
            d_mean, d_std, corr, ks_pval,
            s_new["mean"], is_new=False, th=th,
        )

        # Priority score (0–100): dùng để sort
        score = 0
        if dec == "must":
            score = 60 + min(40, abs(d_mean) / 5)
        elif dec == "should":
            score = 30 + min(29, abs(d_mean) / 3)
        elif dec == "ok":
            score = max(0, 10 - (corr * 10 if corr else 0))
        # Boost bởi RMSE cao (model cũ đã kém → càng nên retrain)
        old_rmse = rmse_map.get(kw, 0)
        if old_rmse and old_rmse > 15:
            score = min(100, score + 5)

        rows.append(dict(
            keyword     = kw,
            decision    = dec,
            priority_score = round(score, 1),
            reasons     = " | ".join(reasons),
            old_mean    = round(s_old["mean"], 2),
            new_mean    = round(s_new["mean"], 2),
            d_mean_pct  = round(d_mean, 1),
            old_std     = round(s_old["std"],  2),
            new_std     = round(s_new["std"],  2),
            d_std_pct   = round(d_std,  1),
            corr_90d    = round(corr, 3) if corr is not None else None,
            ks_pval     = round(ks_pval, 4) if ks_pval is not None else None,
            old_rmse    = old_rmse if old_rmse else None,
        ))

    # --- New keywords ---
    for kw in sorted(added):
        grp_new = df_new[df_new["keyword"] == kw]
        s_new   = compute_stats(grp_new["value"])
        dec, reasons = decide(0, 0, None, None, s_new["mean"], is_new=True, th=th)
        rows.append(dict(
            keyword=kw, decision=dec, priority_score=70,
            reasons=" | ".join(reasons),
            old_mean=None, new_mean=round(s_new["mean"], 2),
            d_mean_pct=None, old_std=None, new_std=round(s_new["std"], 2),
            d_std_pct=None, corr_90d=None, ks_pval=None, old_rmse=None,
        ))

    result = pd.DataFrame(rows)

    # Sort: must first, then by priority_score desc
    order = {"must": 0, "should": 1, "ok": 2, "skip": 3, "dropped": 4}
    result["_order"] = result["decision"].map(order)
    result = result.sort_values(["_order", "priority_score"], ascending=[True, False])
    result = result.drop(columns=["_order"]).reset_index(drop=True)

    return result


def print_summary(result: pd.DataFrame) -> None:
    counts = result["decision"].value_counts()
    print(f"\n{'═'*55}")
    print(f"  KẾT QUẢ DETECT RETRAIN")
    print(f"{'═'*55}")
    for dec in ["must", "should", "ok", "skip", "dropped"]:
        n = counts.get(dec, 0)
        if n == 0:
            continue
        icon = {"must":"🔴","should":"🟡","ok":"🟢","skip":"⚪","dropped":"⛔"}.get(dec,"")
        label = {"must":"Phải retrain","should":"Nên retrain","ok":"Không cần",
                 "skip":"Bỏ qua (volume thấp)","dropped":"Đã xóa"}.get(dec, dec)
        print(f"  {icon}  {label:<25} {n:>3} keywords")
    print(f"{'─'*55}")

    must_kws = result[result.decision=="must"]["keyword"].tolist()
    if must_kws:
        print(f"\n  🔴 Phải retrain ngay:")
        for kw in must_kws:
            row = result[result.keyword==kw].iloc[0]
            print(f"     • {kw:<35} score={row.priority_score}  ({row.reasons})")

    should_kws = result[result.decision=="should"]["keyword"].tolist()
    if should_kws:
        print(f"\n  🟡 Nên retrain (sprint tới):")
        for kw in should_kws:
            row = result[result.keyword==kw].iloc[0]
            print(f"     • {kw:<35} score={row.priority_score}  ({row.reasons})")
    print()


# ─────────────────────────────────────────────────────────────────
# CLI
# ─────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Detect which keywords need retraining after merge")
    parser.add_argument("--data",     required=True,  help="File CSV đã merge (date, keyword, value)")
    parser.add_argument("--prev",     default=None,   help="Snapshot parquet từ lần trước")
    parser.add_argument("--rmse",     default=None,   help="CSV train results (cột: keyword, rmse)")
    parser.add_argument("--snapshot", action="store_true",
                        help="Chỉ lưu snapshot, không so sánh")
    parser.add_argument("--snap-out", default="snapshot.parquet",
                        help="Path lưu snapshot (default: snapshot.parquet)")
    parser.add_argument("--out",      default="retrain_priority.csv",
                        help="Path output CSV (default: retrain_priority.csv)")
    parser.add_argument("--no-log",   action="store_true")
    args = parser.parse_args()

    log = not args.no_log
    ts  = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    print(f"\n{'═'*55}")
    print(f"  detect_retrain.py  [{ts}]")
    print(f"{'═'*55}")

    # Load data
    df = pd.read_csv(args.data, parse_dates=["date"])
    required = {"date", "value", "keyword"}
    if not required.issubset(df.columns):
        print(f"[ERROR] Thiếu cột: {required - set(df.columns)}")
        sys.exit(1)

    # Snapshot-only mode
    if args.snapshot:
        save_snapshot(df, args.snap_out)
        return

    # Cần --prev để so sánh
    if not args.prev:
        print("[ERROR] Cần --prev <snapshot.parquet> để so sánh. "
              "Lần đầu dùng: python detect_retrain.py --snapshot --data <file>")
        sys.exit(1)

    if not Path(args.prev).exists():
        print(f"[ERROR] Không tìm thấy snapshot: {args.prev}")
        sys.exit(1)

    snap = load_snapshot(args.prev)

    # Load RMSE map (optional)
    rmse_map = {}
    if args.rmse and Path(args.rmse).exists():
        rmse_df = pd.read_csv(args.rmse)
        # Hỗ trợ cả cột 'rmse' và 'RMSE'
        rmse_col = next((c for c in rmse_df.columns if c.lower() == "rmse"), None)
        kw_col   = next((c for c in rmse_df.columns if c.lower() == "keyword"), None)
        if rmse_col and kw_col:
            rmse_map = dict(zip(rmse_df[kw_col], rmse_df[rmse_col]))
            print(f"[INFO] Loaded RMSE cho {len(rmse_map)} keywords từ {args.rmse}")

    # Run detection
    result = detect_retrain(df, snap, rmse_map, THRESHOLDS, log=log)

    # Print summary
    print_summary(result)

    # Save output
    result.to_csv(args.out, index=False)
    print(f"[OK] Kết quả lưu tại: {args.out}  ({len(result)} keywords)\n")

    # Update snapshot với data mới
    save_snapshot(df, args.snap_out)
    print(f"[OK] Snapshot cập nhật: {args.snap_out}\n")


if __name__ == "__main__":
    main()