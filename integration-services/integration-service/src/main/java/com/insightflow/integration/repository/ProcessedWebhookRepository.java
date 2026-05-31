package com.insightflow.integration.repository;

import com.insightflow.integration.entity.ProcessedWebhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedWebhookRepository extends JpaRepository<ProcessedWebhook, UUID> {

    boolean existsByConnectorTypeAndExternalEventId(String connectorType, String externalEventId);
}
