package com.insightflow.catalog.repository;

import com.insightflow.catalog.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

    List<Location> findByTenantIdAndIsActiveTrue(UUID tenantId);

    Optional<Location> findByTenantIdAndId(UUID tenantId, UUID id);
}
