package com.insightflow.billing.repository;

import com.insightflow.billing.entity.PlanUpgradeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanUpgradeRequestRepository extends JpaRepository<PlanUpgradeRequest, UUID> {

    List<PlanUpgradeRequest> findByStatusOrderByCreatedAtAsc(String status);

    List<PlanUpgradeRequest> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
