package com.insightflow.billing.repository;

import com.insightflow.billing.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, UUID> {

    Optional<Feature> findByCode(String code);

    List<Feature> findByStatus(String status);

    List<Feature> findByCategory(String category);
}
