package com.insightflow.productcatalog.controller;


import com.insightflow.productcatalog.dto.request.EnrichmentRequest;
import com.insightflow.productcatalog.dto.response.EnrichmentResponse;
import com.insightflow.productcatalog.service.CatalogEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogEnrichmentService enrichmentService;

    /**
     * API bóc tách và chuẩn hóa thuộc tính thời trang từ chuỗi thô
     * URL: POST http://localhost:8083/api/v1/catalog/enrich
     */
    @PostMapping("/enrich")
    public ResponseEntity<EnrichmentResponse> enrichProduct(@RequestBody EnrichmentRequest request) {
        log.info("🤖 [CONTROLLER] Nhận yêu cầu bóc tách dữ liệu cho sản phẩm: {}", request.getProductName());
        EnrichmentResponse response = enrichmentService.enrichProductData(request);
        return ResponseEntity.ok(response);
    }

    /**
     * API cho phép Admin làm mới bộ nhớ đệm RAM Cache ngay lập tức khi DB Dictionary thay đổi
     * URL: POST http://localhost:8083/api/v1/catalog/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshCatalogCache() {
        log.info("🔄 [CONTROLLER] Nhận yêu cầu làm mới RAM Cache từ dữ liệu từ điển...");
        enrichmentService.refreshCache();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Đã làm mới bộ nhớ đệm từ điển thành công từ Database!"
        ));
    }
}