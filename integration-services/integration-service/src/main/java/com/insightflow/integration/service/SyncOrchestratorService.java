package com.insightflow.integration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.integration.connector.kiotviet.KiotVietAuthClient;
import com.insightflow.integration.connector.kiotviet.KiotVietConnector;
import com.insightflow.integration.connector.kiotviet.model.KvBranch;
import com.insightflow.integration.connector.kiotviet.model.KvInventory;
import com.insightflow.integration.connector.kiotviet.model.KvOrder;
import com.insightflow.integration.connector.kiotviet.model.KvProduct;
import com.insightflow.integration.core.ConnectorRegistry;
import com.insightflow.integration.core.ConnectorType;
import com.insightflow.integration.core.CredentialVault;
import com.insightflow.integration.entity.ConnectorConfig;
import com.insightflow.integration.entity.SyncJob;
import com.insightflow.integration.event.producer.IntegrationEventProducer;
import com.insightflow.integration.exception.ConnectorException;
import com.insightflow.integration.exception.ResourceNotFoundException;
import com.insightflow.integration.repository.ConnectorConfigRepository;
import com.insightflow.integration.repository.SyncJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncOrchestratorService {

    private static final int PAGE_SIZE = 100;

    private final ConnectorConfigRepository configRepository;
    private final SyncJobRepository syncJobRepository;
    private final ConnectorRegistry connectorRegistry;
    private final CredentialVault credentialVault;
    private final IntegrationEventProducer eventProducer;
    private final KiotVietAuthClient kiotVietAuthClient;
    private final ObjectMapper objectMapper;

    @Async
    public void triggerFullSync(UUID configId, UUID tenantId) {
        ConnectorConfig config = configRepository.findByIdAndTenantId(configId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Connector config not found: " + configId));
        log.info("Full sync started: config={} tenant={} type={}", configId, tenantId, config.getConnectorType());
        runSync(config, Instant.now().minus(365, ChronoUnit.DAYS), "FULL_RECONCILIATION");
    }

    @Async
    public void triggerIncrementalSync(UUID configId, UUID tenantId) {
        ConnectorConfig config = configRepository.findByIdAndTenantId(configId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Connector config not found: " + configId));
        Instant since = config.getLastSyncAt() != null
                ? config.getLastSyncAt()
                : Instant.now().minus(30, ChronoUnit.DAYS);
        log.info("Incremental sync started: config={} tenant={} since={}", configId, tenantId, since);
        runSync(config, since, "INCREMENTAL");
    }

    public SyncJob createQueuedJob(UUID tenantId, UUID configId, String syncType) {
        SyncJob job = new SyncJob();
        job.setTenantId(tenantId);
        job.setConnectorConfigId(configId);
        job.setEntityType("ALL");
        job.setSyncType(syncType);
        job.setStatus("queued");
        return syncJobRepository.save(job);
    }

    public Page<SyncJob> getSyncJobs(UUID configId, UUID tenantId, Pageable pageable) {
        return syncJobRepository.findByConnectorConfigIdAndTenantId(configId, tenantId, pageable);
    }

    private void runSync(ConnectorConfig config, Instant since, String syncType) {
        if (config.getConnectorType() != ConnectorType.KIOTVIET) {
            log.warn("Connector type {} not yet implemented, skipping", config.getConnectorType());
            return;
        }

        SyncJob job = new SyncJob();
        job.setTenantId(config.getTenantId());
        job.setConnectorConfigId(config.getId());
        job.setEntityType("ALL");
        job.setSyncType(syncType);
        job.setStatus("running");
        job.setStartedAt(Instant.now());
        job = syncJobRepository.save(job);

        try {
            Map<String, String> credentials = decryptCredentials(config);
            String clientId = credentials.get("clientId");
            String clientSecret = credentials.get("clientSecret");
            String retailerName = credentials.getOrDefault("retailerName", "");
            String token = kiotVietAuthClient.getAccessToken(clientId, clientSecret);

            KiotVietConnector connector = (KiotVietConnector) connectorRegistry.get(ConnectorType.KIOTVIET);
            int totalSynced = 0;

            // Sync branches (locations)
            List<KvBranch> branches = connector.fetchBranches(token, retailerName);
            log.info("Synced {} branches for tenant={}", branches.size(), config.getTenantId());
            totalSynced += branches.size();

            // Sync products (paginated)
            int productOffset = 0;
            List<KvProduct> products;
            do {
                products = connector.fetchProducts(token, retailerName, productOffset, PAGE_SIZE);
                if (!products.isEmpty()) {
                    eventProducer.publishProductSynced(config.getTenantId(), config.getId(), products);
                    totalSynced += products.size();
                }
                productOffset += products.size();
            } while (products.size() == PAGE_SIZE);
            log.info("Synced {} products for tenant={}", productOffset, config.getTenantId());

            // Sync orders (paginated, from watermark)
            int orderOffset = 0;
            List<KvOrder> orders;
            do {
                orders = connector.fetchOrders(token, retailerName, since, orderOffset, PAGE_SIZE);
                if (!orders.isEmpty()) {
                    eventProducer.publishOrderSynced(config.getTenantId(), config.getId(), orders);
                    totalSynced += orders.size();
                }
                orderOffset += orders.size();
            } while (orders.size() == PAGE_SIZE);
            log.info("Synced {} orders for tenant={}", orderOffset, config.getTenantId());

            // Sync inventory per branch
            for (KvBranch branch : branches) {
                int invOffset = 0;
                List<KvInventory> inventory;
                do {
                    inventory = connector.fetchInventory(token, retailerName, branch.getId(), invOffset, PAGE_SIZE);
                    if (!inventory.isEmpty()) {
                        eventProducer.publishInventorySynced(config.getTenantId(), config.getId(), inventory);
                        totalSynced += inventory.size();
                    }
                    invOffset += inventory.size();
                } while (inventory.size() == PAGE_SIZE);
            }

            // Publish completion event
            eventProducer.publishSyncCompleted(config.getTenantId(), config.getId(),
                    config.getConnectorType().name(), syncType, totalSynced);

            // Update config watermark
            config.setLastSyncAt(Instant.now());
            config.setLastError(null);
            configRepository.save(config);

            // Complete job
            job.setStatus("success");
            job.setRecordsProcessed(totalSynced);
            job.setCompletedAt(Instant.now());
            syncJobRepository.save(job);

            log.info("Sync completed: config={} totalSynced={}", config.getId(), totalSynced);

        } catch (Exception e) {
            log.error("Sync failed for config={}: {}", config.getId(), e.getMessage(), e);
            config.setLastError(e.getMessage());
            configRepository.save(config);

            job.setStatus("failed");
            job.setCompletedAt(Instant.now());
            job.setErrorLog(Map.of("error", e.getMessage() != null ? e.getMessage() : "unknown"));
            syncJobRepository.save(job);
        }
    }

    private Map<String, String> decryptCredentials(ConnectorConfig config) {
        try {
            String json = credentialVault.decrypt(config.getCredentialsEncrypted());
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new ConnectorException("Failed to decrypt credentials for config: " + config.getId(), e);
        }
    }
}
