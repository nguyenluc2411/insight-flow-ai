"""Map raw category slug/name from catalog-service to a base-model category_key.

The base-model taxonomy is a fixed set of 14 keys owned entirely by ml-service —
catalog-service does not know this list and only emits the shop's raw category
(name + slug) on ``catalog.inventory.updated``. Any taxonomy change touches this
file only, never catalog-service.

Valid category_key values:
    ao_so_mi, vay_dam, quan_jeans, ao_thun, giay_sneaker, ao_khoac,
    tui_xach, dam_maxi, quan_au, do_the_thao,
    dam_cong_so, ao_dai, chan_vay, phu_kien
"""

from __future__ import annotations

# Normalized slug/name (lowercased) → base-model category_key.
CATALOG_TO_BASE_MODEL: dict[str, str] = {
    # ao_so_mi
    "ao-so-mi": "ao_so_mi",
    "ao_so_mi": "ao_so_mi",
    "ao-so-mi-nam": "ao_so_mi",
    "ao-so-mi-nu": "ao_so_mi",
    "shirt": "ao_so_mi",
    # vay_dam
    "vay-dam": "vay_dam",
    "vay_dam": "vay_dam",
    "dam-du-tiec": "vay_dam",
    "dam_du_tiec": "vay_dam",
    "vay-midi": "vay_dam",
    "dress": "vay_dam",
    # quan_jeans
    "quan-jeans": "quan_jeans",
    "quan_jeans": "quan_jeans",
    "quan-denim": "quan_jeans",
    "jeans": "quan_jeans",
    "denim": "quan_jeans",
    # ao_thun
    "ao-thun": "ao_thun",
    "ao_thun": "ao_thun",
    "ao-phong": "ao_thun",
    "tshirt": "ao_thun",
    "t-shirt": "ao_thun",
    # giay_sneaker
    "giay-sneaker": "giay_sneaker",
    "giay_sneaker": "giay_sneaker",
    "giay-the-thao": "giay_sneaker",
    "sneaker": "giay_sneaker",
    # ao_khoac
    "ao-khoac": "ao_khoac",
    "ao_khoac": "ao_khoac",
    "ao-hoodie": "ao_khoac",
    "ao-bomber": "ao_khoac",
    "jacket": "ao_khoac",
    "hoodie": "ao_khoac",
    # tui_xach
    "tui-xach": "tui_xach",
    "tui_xach": "tui_xach",
    "tui-tote": "tui_xach",
    "tui-deo-cheo": "tui_xach",
    "bag": "tui_xach",
    "handbag": "tui_xach",
    # dam_maxi
    "dam-maxi": "dam_maxi",
    "dam_maxi": "dam_maxi",
    "vay-maxi": "dam_maxi",
    "dam-dai": "dam_maxi",
    "maxi": "dam_maxi",
    # quan_au
    "quan-au": "quan_au",
    "quan_au": "quan_au",
    "quan-tay": "quan_au",
    "trouser": "quan_au",
    # do_the_thao
    "do-the-thao": "do_the_thao",
    "do_the_thao": "do_the_thao",
    "sportswear": "do_the_thao",
    "activewear": "do_the_thao",
    # dam_cong_so
    "dam-cong-so": "dam_cong_so",
    "dam_cong_so": "dam_cong_so",
    "vay-cong-so": "dam_cong_so",
    "cong-so": "dam_cong_so",
    "office_dress": "dam_cong_so",
    # ao_dai
    "ao-dai": "ao_dai",
    "ao_dai": "ao_dai",
    "ao-dai-cach-tan": "ao_dai",
    "aodai": "ao_dai",
    # chan_vay
    "chan-vay": "chan_vay",
    "chan_vay": "chan_vay",
    "vay-chu-a": "chan_vay",
    "vay-suong": "chan_vay",
    "skirt": "chan_vay",
    # phu_kien
    "phu-kien": "phu_kien",
    "phu_kien": "phu_kien",
    "accessory": "phu_kien",
    "accessories": "phu_kien",
    "phu-kien-thoi-trang": "phu_kien",
}


def resolve_category_key(
    category_slug: str | None,
    category_name: str | None = None,
) -> str:
    """Map a raw catalog slug/name to a base-model category_key.

    Prefers the slug (URL-friendly, more stable); falls back to the name
    normalised to slug shape.

    Args:
        category_slug: Slug from catalog, e.g. "ao-so-mi-nam".
        category_name: Display name from catalog, e.g. "Áo Sơ Mi Nam" (fallback).

    Returns:
        A valid category_key, or "unknown" when nothing maps.
    """

    def _normalize(value: str) -> str:
        return value.lower().strip()

    if category_slug:
        key = CATALOG_TO_BASE_MODEL.get(_normalize(category_slug))
        if key:
            return key

    if category_name:
        normalized = _normalize(category_name).replace(" ", "-").replace("_", "-")
        key = CATALOG_TO_BASE_MODEL.get(normalized)
        if key:
            return key

    return "unknown"
