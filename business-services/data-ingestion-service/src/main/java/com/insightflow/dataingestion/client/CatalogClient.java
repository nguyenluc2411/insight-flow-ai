package com.insightflow.dataingestion.client;

import com.insightflow.dataingestion.dto.request.ColumnResolveRequest;
import com.insightflow.dataingestion.dto.request.EnrichmentRequest;
import com.insightflow.dataingestion.dto.response.EnrichmentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "product-catalog-service", url = "${catalog-service.base-url}")
public interface CatalogClient {

    @PostMapping("/api/v1/catalog/enrich")
    EnrichmentResponse enrichProduct(@RequestBody EnrichmentRequest request);

    /** Phân giải tên cột thô của file upload -> trường chuẩn (header gốc -> field). */
    @PostMapping("/api/v1/catalog/resolve-columns")
    Map<String, String> resolveColumns(@RequestBody ColumnResolveRequest request);
}