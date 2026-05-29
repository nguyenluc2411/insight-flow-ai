package com.insightflow.integration.repository;

import com.insightflow.integration.entity.ConnectorConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectorConfigRepository extends JpaRepository<ConnectorConfig, UUID> {

    List<ConnectorConfig> findByTenantId(UUID tenantId);

    Optional<ConnectorConfig> findByIdAndTenantId(UUID id, UUID tenantId);

    List<ConnectorConfig> findByStatus(String status);

    boolean existsByTenantIdAndConnectorType(UUID tenantId,
            com.insightflow.integration.core.ConnectorType connectorType);
}
