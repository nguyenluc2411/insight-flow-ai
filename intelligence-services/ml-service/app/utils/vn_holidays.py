"""Vietnamese holiday & shopping-event calendar for Prophet (2022–2026).

Fashion demand in Ho Chi Minh City swings sharply around Tết, the two Women's
Days, the mega-sale days (11/11, Black Friday) and Christmas. Passing these as
Prophet's ``holidays`` argument lets the model attribute those spikes to the
events instead of mis-learning them as trend/seasonality.

Holiday effects in Prophet are modelled as additional regressors (dummy
variables) around each date — Taylor & Letham (2018),
"Forecasting at Scale", doi:10.7287/peerj.preprints.3190v2.

Sources:
  - Official VN public holidays: Cổng thông tin Chính phủ Việt Nam
    https://chinhphu.vn
  - Lunar New Year (Tết) Gregorian dates (looked up, not computed):
    https://www.timeanddate.com/holidays/vietnam/
"""

from __future__ import annotations

import pandas as pd

_YEARS: tuple[int, ...] = (2022, 2023, 2024, 2025, 2026)

# Tết Nguyên Đán — lunar calendar, converted to Gregorian via lookup table
# (do NOT compute). Demand ramps up ~10 days before and tapers a few days after.
# Source: https://www.timeanddate.com/holidays/vietnam/
_TET_DATES: dict[int, str] = {
    2022: "2022-02-01",
    2023: "2023-01-22",
    2024: "2024-02-10",
    2025: "2025-01-29",
    2026: "2026-02-17",
}


def _last_friday_of_november(year: int) -> pd.Timestamp:
    """Black Friday = the last Friday of November for the given year."""
    nov_end = pd.Timestamp(year=year, month=11, day=30)
    offset = (nov_end.weekday() - 4) % 7  # weekday(): Mon=0 … Fri=4
    return nov_end - pd.Timedelta(days=offset)


def _holiday_frame(
    name: str, dates: list, lower_window: int, upper_window: int
) -> pd.DataFrame:
    """Build a Prophet-compatible holiday frame for one event across all years."""
    return pd.DataFrame(
        {
            "holiday": name,
            "ds": pd.to_datetime(list(dates)),
            "lower_window": lower_window,
            "upper_window": upper_window,
        }
    )


def _build_vn_holidays() -> pd.DataFrame:
    """Assemble the full VN holiday calendar used by every Prophet model."""
    frames: list[pd.DataFrame] = [
        # Tết Nguyên Đán — biggest fashion spike of the year (áo dài, đồ mới).
        # Source: timeanddate.com (lunar→Gregorian), chinhphu.vn (official break)
        _holiday_frame("tet_nguyen_dan", [_TET_DATES[y] for y in _YEARS], -10, 3),
        # International Women's Day 8/3 & Vietnamese Women's Day 20/10 —
        # strong womenswear / accessories / gifting demand.
        # Source: chinhphu.vn
        _holiday_frame("quoc_te_phu_nu_8_3", [f"{y}-03-08" for y in _YEARS], -3, 1),
        _holiday_frame("phu_nu_viet_nam_20_10", [f"{y}-10-20" for y in _YEARS], -3, 1),
        # E-commerce mega-sale days driving fashion purchases in VN.
        # Source: Shopee/Lazada/TikTok Shop VN campaign calendar
        _holiday_frame("double_11", [f"{y}-11-11" for y in _YEARS], -2, 1),
        _holiday_frame(
            "black_friday", [_last_friday_of_november(y) for y in _YEARS], -2, 2
        ),
        # Christmas (24–25/12) — gifting + party outfits; long ramp-up.
        # Valentine 14/2 — couples gifting.
        # Source: chinhphu.vn / retail seasonal calendar
        _holiday_frame("giang_sinh", [f"{y}-12-25" for y in _YEARS], -5, 1),
        _holiday_frame("valentine", [f"{y}-02-14" for y in _YEARS], -2, 0),
    ]
    return pd.concat(frames, ignore_index=True)


# Prophet ``holidays`` DataFrame: columns [holiday, ds, lower_window, upper_window].
VN_HOLIDAYS: pd.DataFrame = _build_vn_holidays()
