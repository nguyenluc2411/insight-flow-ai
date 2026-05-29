package com.insightflow.integration.controller;

import com.insightflow.integration.dto.request.CreateConnectorRequest;
import com.insightflow.integration.dto.response.ConnectorConfigResponse;
import com.insightflow.integration.dto.response.SyncJobResponse;
import com.insightflow.integration.entity.SyncJob;
import com.insightflow.integration.mapper.SyncJobMapper;
import com.insightflow.integration.service.ConnectorConfigService;
import com.insightflow.integration.service.SyncOrchestratorService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
@Tag(name = "Integrations", description = "POS connector management")
public class IntegrationController {

    private final ConnectorConfigService configService;
    private final SyncOrchestratorService orchestrator;
    private final SyncJobMapper syncJobMapper;

    @GetMapping
    @Operation(summary = "List connector configs for the tenant")
    @ApiResponse(responseCode = "200", description = "List of connectors")
    public ResponseEntity<List<ConnectorConfigResponse>> list(@CurrentUser UserContext user) {
        return ResponseEntity.ok(configService.getConfigs(user.tenantId()));
    }

    @PostMapping
    @Operation(summary = "Create a new POS connector")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Connector created"),
            @ApiResponse(responseCode = "400", description = "Validation error or auth failed"),
            @ApiResponse(responseCode = "409", description = "Connector type already exists")
    })
    public ResponseEntity<ConnectorConfigResponse> create(
            @CurrentUser UserContext user,
            @Valid @RequestBody CreateConnectorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configService.createConfig(user.tenantId(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get connector config by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connector found"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ConnectorConfigResponse> getById(
            @CurrentUser UserContext user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(configService.getConfig(id, user.tenantId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate connector config")
    @ApiResponse(responseCode = "204", description = "Deactivated")
    public ResponseEntity<Void> delete(
            @CurrentUser UserContext user,
            @PathVariable UUID id) {
        configService.deleteConfig(id, user.tenantId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "Trigger manual full sync",
               description = "Asynchronous — returns job ID immediately")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Sync job queued"),
            @ApiResponse(responseCode = "404", description = "Connector not found")
    })
    public ResponseEntity<Map<String, Object>> triggerSync(
            @CurrentUser UserContext user,
            @PathVariable UUID id) {
        configService.getConfig(id, user.tenantId());

        SyncJob job = orchestrator.createQueuedJob(user.tenantId(), id, "FULL_RECONCILIATION");
        orchestrator.triggerFullSync(id, user.tenantId());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of(
                        "jobId", job.getId().toString(),
                        "status", "queued",
                        "message", "Full sync triggered asynchronously"
                ));
    }

    @GetMapping("/{id}/jobs")
    @Operation(summary = "List sync jobs for a connector")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of sync jobs"),
            @ApiResponse(responseCode = "404", description = "Connector not found")
    })
    public ResponseEntity<Page<SyncJobResponse>> getSyncJobs(
            @CurrentUser UserContext user,
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        configService.getConfig(id, user.tenantId());
        return ResponseEntity.ok(
                orchestrator.getSyncJobs(id, user.tenantId(), pageable)
                        .map(syncJobMapper::toResponse));
    }
}
