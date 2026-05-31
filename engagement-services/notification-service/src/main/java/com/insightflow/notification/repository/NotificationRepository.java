package com.insightflow.notification.repository;

import com.insightflow.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<Notification> findByTenantIdAndIsReadFalseOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<Notification> findByTenantIdAndTypeOrderByCreatedAtDesc(UUID tenantId, String type, Pageable pageable);

    long countByTenantIdAndIsReadFalse(UUID tenantId);

    Optional<Notification> findByIdAndTenantId(UUID id, UUID tenantId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.tenantId = :tenantId AND n.isRead = false")
    int markAllReadByTenantId(UUID tenantId);
}
