-- =============================================================================
-- V2 — Column-mapping dictionary (header synonym -> canonical ingestion field).
-- Reuses the existing catalog_dictionaries engine: attribute_type = 'COLUMN_MAPPING',
-- synonym = a NORMALIZED header key (lowercase, no diacritics, snake_case — the same
-- shape DynamicFileParser produces), standard_value = the canonical field name that
-- data-ingestion writes into Product / ProductVariant / InventoryFact.
--
-- Coverage strategy (so user data is never dropped after parsing):
--   * This list only needs to cover different WORDINGS (Vietnamese + English).
--   * Format variants are handled by the resolver, NOT by listing them here:
--       - diacritics      -> stripped on both sides before matching ("Màu" = "mau")
--       - underscore/space/joined -> "compact" match ignores '_' ("mau_sac" = "mausac")
--       - typos/near-miss -> Levenshtein fuzzy fallback
--   * Add more wordings anytime: INSERT a row here (or in the table) + POST /refresh.
-- =============================================================================
SET search_path TO enrichment_db;

INSERT INTO catalog_dictionaries (attribute_type, standard_value, synonym) VALUES
-- product_name
('COLUMN_MAPPING','product_name','ten_san_pham'),
('COLUMN_MAPPING','product_name','ten_sp'),
('COLUMN_MAPPING','product_name','ten_hang'),
('COLUMN_MAPPING','product_name','ten_hang_hoa'),
('COLUMN_MAPPING','product_name','ten_mat_hang'),
('COLUMN_MAPPING','product_name','san_pham'),
('COLUMN_MAPPING','product_name','mat_hang'),
('COLUMN_MAPPING','product_name','ten'),
('COLUMN_MAPPING','product_name','tieu_de'),
('COLUMN_MAPPING','product_name','name'),
('COLUMN_MAPPING','product_name','product_name'),
('COLUMN_MAPPING','product_name','product'),
('COLUMN_MAPPING','product_name','item_name'),
('COLUMN_MAPPING','product_name','title'),
-- product_code
('COLUMN_MAPPING','product_code','ma_san_pham'),
('COLUMN_MAPPING','product_code','ma_sp'),
('COLUMN_MAPPING','product_code','ma_hang'),
('COLUMN_MAPPING','product_code','ma_hang_hoa'),
('COLUMN_MAPPING','product_code','ma'),
('COLUMN_MAPPING','product_code','code'),
('COLUMN_MAPPING','product_code','product_code'),
('COLUMN_MAPPING','product_code','item_code'),
('COLUMN_MAPPING','product_code','product_id'),
-- sku
('COLUMN_MAPPING','sku','sku'),
('COLUMN_MAPPING','sku','ma_sku'),
('COLUMN_MAPPING','sku','ma_vach'),
('COLUMN_MAPPING','sku','ma_vach_sp'),
('COLUMN_MAPPING','sku','barcode'),
('COLUMN_MAPPING','sku','ma_barcode'),
('COLUMN_MAPPING','sku','variant_sku'),
-- category
('COLUMN_MAPPING','category','danh_muc'),
('COLUMN_MAPPING','category','danh_muc_sp'),
('COLUMN_MAPPING','category','loai'),
('COLUMN_MAPPING','category','loai_sp'),
('COLUMN_MAPPING','category','loai_hang'),
('COLUMN_MAPPING','category','nhom_hang'),
('COLUMN_MAPPING','category','nganh_hang'),
('COLUMN_MAPPING','category','phan_loai'),
('COLUMN_MAPPING','category','category'),
('COLUMN_MAPPING','category','category_name'),
('COLUMN_MAPPING','category','type'),
-- color
('COLUMN_MAPPING','color','mau'),
('COLUMN_MAPPING','color','mau_sac'),
('COLUMN_MAPPING','color','mau_san_pham'),
('COLUMN_MAPPING','color','color'),
('COLUMN_MAPPING','color','color_name'),
('COLUMN_MAPPING','color','colour'),
-- size
('COLUMN_MAPPING','size','size'),
('COLUMN_MAPPING','size','co'),
('COLUMN_MAPPING','size','kich_co'),
('COLUMN_MAPPING','size','kich_thuoc'),
('COLUMN_MAPPING','size','size_eu'),
('COLUMN_MAPPING','size','size_us'),
-- stock
('COLUMN_MAPPING','stock','ton_kho'),
('COLUMN_MAPPING','stock','ton'),
('COLUMN_MAPPING','stock','ton_hien_tai'),
('COLUMN_MAPPING','stock','ton_kho_hien_tai'),
('COLUMN_MAPPING','stock','so_luong_ton'),
('COLUMN_MAPPING','stock','sl_ton'),
('COLUMN_MAPPING','stock','so_luong'),
('COLUMN_MAPPING','stock','sl'),
('COLUMN_MAPPING','stock','quantity'),
('COLUMN_MAPPING','stock','qty'),
('COLUMN_MAPPING','stock','stock'),
('COLUMN_MAPPING','stock','inventory'),
('COLUMN_MAPPING','stock','on_hand'),
-- cost_price
('COLUMN_MAPPING','cost_price','gia_von'),
('COLUMN_MAPPING','cost_price','gia_nhap'),
('COLUMN_MAPPING','cost_price','gia_nhap_hang'),
('COLUMN_MAPPING','cost_price','gia_goc'),
('COLUMN_MAPPING','cost_price','cost_price'),
('COLUMN_MAPPING','cost_price','cost'),
('COLUMN_MAPPING','cost_price','import_price'),
-- retail_price
('COLUMN_MAPPING','retail_price','gia_ban'),
('COLUMN_MAPPING','retail_price','gia_ban_le'),
('COLUMN_MAPPING','retail_price','gia_le'),
('COLUMN_MAPPING','retail_price','don_gia'),
('COLUMN_MAPPING','retail_price','gia_niem_yet'),
('COLUMN_MAPPING','retail_price','gia'),
('COLUMN_MAPPING','retail_price','retail_price'),
('COLUMN_MAPPING','retail_price','price'),
('COLUMN_MAPPING','retail_price','selling_price'),
('COLUMN_MAPPING','retail_price','sale_price'),
-- import_date
('COLUMN_MAPPING','import_date','ngay_nhap'),
('COLUMN_MAPPING','import_date','ngay_nhap_hang'),
('COLUMN_MAPPING','import_date','ngay_nhap_kho'),
('COLUMN_MAPPING','import_date','import_date'),
('COLUMN_MAPPING','import_date','ngay'),
('COLUMN_MAPPING','import_date','date');
