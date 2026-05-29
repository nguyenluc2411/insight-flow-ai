package com.insightflow.notification.repository;

import com.insightflow.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByTenantId(UUID tenantId);

    Optional<NotificationPreference> findByTenantIdAndEventTypeAndChannel(
            UUID tenantId, String eventType, String channel);
}
