package com.insightflow.billing.repository;

import com.insightflow.billing.entity.PlanLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanLimitRepository extends JpaRepository<PlanLimit, UUID> {

    Optional<PlanLimit> findByPackageId(UUID packageId);
}
