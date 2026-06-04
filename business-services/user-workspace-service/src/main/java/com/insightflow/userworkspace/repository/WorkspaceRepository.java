package com.insightflow.userworkspace.repository;


import com.insightflow.userworkspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, String> {

    // Tenant-scoped lookups — never load a workspace without its owning tenant.
    Optional<Workspace> findByIdAndTenantId(String id, String tenantId);

    List<Workspace> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}