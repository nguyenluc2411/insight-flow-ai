package com.insightflow.auth.repository;

import com.insightflow.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<User> findAllByTenantId(UUID tenantId, Pageable pageable);
}
