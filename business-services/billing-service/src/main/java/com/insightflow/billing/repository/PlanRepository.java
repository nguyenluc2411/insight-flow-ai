package com.insightflow.billing.repository;

import com.insightflow.billing.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

    List<Plan> findByPackageIdAndStatus(UUID packageId, String status);

    List<Plan> findByPackageId(UUID packageId);

    Optional<Plan> findByPackageIdAndBillingCycle(UUID packageId, String billingCycle);

    List<Plan> findByStatus(String status);
}
