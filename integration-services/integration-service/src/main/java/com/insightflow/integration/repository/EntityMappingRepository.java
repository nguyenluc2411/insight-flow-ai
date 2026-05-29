package com.insightflow.integration.repository;

import com.insightflow.integration.entity.EntityMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EntityMappingRepository extends JpaRepository<EntityMapping, Long> {

    Optional<EntityMapping> findByTenantIdAndConnectorConfigIdAndEntityTypeAndExternalId(
            UUID tenantId, UUID connectorConfigId, String entityType, String externalId);

    Optional<EntityMapping> findByTenantIdAndEntityTypeAndInternalId(
            UUID tenantId, String entityType, UUID internalId);
}
