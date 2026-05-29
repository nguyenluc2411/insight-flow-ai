package com.insightflow.billing.repository;

import com.insightflow.billing.entity.PackageFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PackageFeatureRepository extends JpaRepository<PackageFeature, UUID> {

    List<PackageFeature> findByPackageId(UUID packageId);

    List<PackageFeature> findByFeatureId(UUID featureId);
}
