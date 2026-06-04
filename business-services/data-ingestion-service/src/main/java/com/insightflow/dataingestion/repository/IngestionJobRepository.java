package com.insightflow.dataingestion.repository;

import com.insightflow.dataingestion.entity.IngestionJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, String> {
    Optional<IngestionJob> findByWorkspaceId(String workspaceId);
}