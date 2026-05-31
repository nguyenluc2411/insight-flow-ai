package com.insightflow.integration.repository;

import com.insightflow.integration.entity.SyncJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SyncJobRepository extends JpaRepository<SyncJob, UUID> {

    Page<SyncJob> findByConnectorConfigIdAndTenantId(UUID connectorConfigId, UUID tenantId, Pageable pageable);

    Page<SyncJob> findByTenantId(UUID tenantId, Pageable pageable);
}
