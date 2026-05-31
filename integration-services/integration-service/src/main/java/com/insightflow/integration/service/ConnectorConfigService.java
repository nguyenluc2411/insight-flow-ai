package com.insightflow.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.integration.core.ConnectorInterface;
import com.insightflow.integration.core.ConnectorRegistry;
import com.insightflow.integration.core.ConnectorType;
import com.insightflow.integration.core.CredentialVault;
import com.insightflow.integration.dto.request.CreateConnectorRequest;
import com.insightflow.integration.dto.response.ConnectorConfigResponse;
import com.insightflow.integration.entity.ConnectorConfig;
import com.insightflow.common.web.exception.BusinessException;
import com.insightflow.common.web.exception.ErrorCode;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import com.insightflow.integration.mapper.ConnectorConfigMapper;
import com.insightflow.integration.repository.ConnectorConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectorConfigService {

    private final ConnectorConfigRepository repository;
    private final ConnectorRegistry connectorRegistry;
    private final CredentialVault credentialVault;
    private final ConnectorConfigMapper mapper;
    private final ObjectMapper objectMapper;
    private final SyncOrchestratorService syncOrchestrator;

    @Transactional
    public ConnectorConfigResponse createConfig(UUID tenantId, CreateConnectorRequest req) {
        if (repository.existsByTenantIdAndConnectorType(tenantId, req.getConnectorType())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                    "Connector of type " + req.getConnectorType() + " already exists for this tenant");
        }

        ConnectorInterface connector = connectorRegistry.get(req.getConnectorType());
        boolean authenticated = connector.authenticate(req.getCredentials());
        if (!authenticated) {
            throw new BusinessException(ErrorCode.DOWNSTREAM_ERROR,
                    "Authentication failed for connector type: " + req.getConnectorType() +
                    ". Check your credentials.");
        }

        String credentialsJson;
        try {
            credentialsJson = objectMapper.writeValueAsString(req.getCredentials());
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to serialize credentials");
        }

        ConnectorConfig config = new ConnectorConfig();
        config.setTenantId(tenantId);
        config.setConnectorType(req.getConnectorType());
        config.setName(req.getName());
        config.setStatus("active");
        config.setCredentialsEncrypted(credentialVault.encrypt(credentialsJson));
        config.setConfig(req.getConfig());

        ConnectorConfig saved = repository.save(config);
        log.info("ConnectorConfig created: id={} tenant={} type={}", saved.getId(), tenantId, req.getConnectorType());

        // Kick off the initial historical backfill once the config is committed, so a
        // newly-connected tenant gets ~365 days of sales flowing to ML during the trial.
        final UUID configId = saved.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                syncOrchestrator.triggerFullSync(configId, tenantId);
            }
        });

        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConnectorConfigResponse> getConfigs(UUID tenantId) {
        return repository.findByTenantId(tenantId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConnectorConfigResponse getConfig(UUID id, UUID tenantId) {
        return mapper.toResponse(findOrThrow(id, tenantId));
    }

    @Transactional
    public void deleteConfig(UUID id, UUID tenantId) {
        ConnectorConfig config = findOrThrow(id, tenantId);
        config.setStatus("inactive");
        config.setUpdatedAt(Instant.now());
        repository.save(config);
        log.info("ConnectorConfig deactivated: id={}", id);
    }

    ConnectorConfig findOrThrow(UUID id, UUID tenantId) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Connector config not found: " + id));
    }
}
