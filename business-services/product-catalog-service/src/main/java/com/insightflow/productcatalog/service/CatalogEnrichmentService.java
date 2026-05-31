package com.insightflow.productcatalog.service;

import com.insightflow.productcatalog.dto.request.EnrichmentRequest;
import com.insightflow.productcatalog.dto.response.EnrichmentResponse;

public interface CatalogEnrichmentService {

    /**
     * Bóc tách và làm giàu dữ liệu sản phẩm từ chuỗi thô
     */
    EnrichmentResponse enrichProductData(EnrichmentRequest request);

    /**
     * Làm mới bộ nhớ đệm Cache từ Database
     */
    void refreshCache();
}