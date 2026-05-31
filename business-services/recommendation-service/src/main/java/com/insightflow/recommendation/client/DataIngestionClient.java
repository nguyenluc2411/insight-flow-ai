package com.insightflow.recommendation.client;

import com.insightflow.recommendation.dto.response.WorkspaceInventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Trỏ về cổng của service 8082 để lấy data
@FeignClient(name = "data-ingestion-service", url = "${ingestion-service.base-url:http://localhost:8082}")
public interface DataIngestionClient {
    @GetMapping("/api/v1/inventories/workspace/{id}")
    WorkspaceInventoryResponse exportWorkspaceData(@PathVariable("id") String workspaceId);
}