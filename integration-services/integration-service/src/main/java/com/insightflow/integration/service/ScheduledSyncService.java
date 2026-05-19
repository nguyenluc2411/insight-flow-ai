package com.insightflow.integration.service;

import com.insightflow.integration.entity.ConnectorConfig;
import com.insightflow.integration.repository.ConnectorConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledSyncService {

    private final ConnectorConfigRepository configRepository;
    private final SyncOrchestratorService orchestrator;

    @Scheduled(fixedDelayString = "${app.integration.scheduled.incremental-sync-delay-ms:900000}")
    public void incrementalSyncAll() {
        List<ConnectorConfig> active = configRepository.findByStatus("active");
        if (active.isEmpty()) {
            return;
        }
        log.info("Incremental sync scheduled: {} active connectors", active.size());
        for (ConnectorConfig config : active) {
            try {
                orchestrator.triggerIncrementalSync(config.getId(), config.getTenantId());
            } catch (Exception e) {
                log.error("Incremental sync failed for config={}: {}", config.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${app.integration.scheduled.full-reconciliation-cron:0 0 2 * * *}")
    public void fullReconciliationAll() {
        List<ConnectorConfig> active = configRepository.findByStatus("active");
        if (active.isEmpty()) {
            return;
        }
        log.info("Full reconciliation scheduled: {} active connectors", active.size());
        for (ConnectorConfig config : active) {
            try {
                orchestrator.triggerFullSync(config.getId(), config.getTenantId());
            } catch (Exception e) {
                log.error("Full reconciliation failed for config={}: {}", config.getId(), e.getMessage());
            }
        }
    }
}
