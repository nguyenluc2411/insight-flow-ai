package com.insightflow.dataingestion.client;

import com.insightflow.dataingestion.dto.request.EnrichmentRequest;
import com.insightflow.dataingestion.dto.response.EnrichmentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-catalog-service", url = "${catalog-service.base-url}")
public interface CatalogClient {

    @PostMapping("/api/v1/catalog/enrich")
    EnrichmentResponse enrichProduct(@RequestBody EnrichmentRequest request);
}