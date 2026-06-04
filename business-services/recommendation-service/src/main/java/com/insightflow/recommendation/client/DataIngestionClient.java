package com.insightflow.recommendation.client;

import com.insightflow.recommendation.dto.response.WorkspaceInventoryResponse;
import com.insightflow.security.InternalHeaders;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "data-ingestion-service", url = "${ingestion-service.base-url:http://localhost:8088}")
public interface DataIngestionClient {

    // Pass tenant explicitly: this is a service-to-service call (no gateway),
    // and data-ingestion scopes the export by X-Tenant-Id.
    @GetMapping("/api/v1/inventories/workspace/{id}")
    WorkspaceInventoryResponse exportWorkspaceData(@PathVariable("id") String workspaceId,
                                                   @RequestHeader(InternalHeaders.X_TENANT_ID) String tenantId);
}