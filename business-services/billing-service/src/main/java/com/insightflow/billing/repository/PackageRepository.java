package com.insightflow.billing.repository;

import com.insightflow.billing.entity.BillingPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PackageRepository extends JpaRepository<BillingPackage, UUID> {

    List<BillingPackage> findByStatusOrderByDisplayOrderAsc(String status);

    Optional<BillingPackage> findByCodeAndStatus(String code, String status);

    Optional<BillingPackage> findByCode(String code);
}
