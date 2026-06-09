package com.insightflow.productcatalog.service;

import com.insightflow.productcatalog.dto.request.EnrichmentRequest;
import com.insightflow.productcatalog.dto.response.EnrichmentResponse;

import java.util.List;
import java.util.Map;

public interface CatalogEnrichmentService {

    /**
     * Bóc tách và làm giàu dữ liệu sản phẩm từ chuỗi thô
     */
    EnrichmentResponse enrichProductData(EnrichmentRequest request);

    /**
     * Phân giải tên cột thô của file upload về trường chuẩn (product_name, stock,
     * retail_price...). Khử dấu + bỏ gạch dưới + fuzzy nên bao được tên cột đa dạng.
     * Trả về map: header gốc -> tên trường chuẩn (bỏ qua header không nhận diện được).
     */
    Map<String, String> resolveColumns(List<String> headers);

    /**
     * Làm mới bộ nhớ đệm Cache từ Database
     */
    void refreshCache();
}