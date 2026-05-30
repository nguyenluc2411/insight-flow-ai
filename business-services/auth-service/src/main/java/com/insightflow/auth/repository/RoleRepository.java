package com.insightflow.auth.repository;

import com.insightflow.auth.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);

    // Eagerly load permissions to avoid N+1 when building JWT claims
    @EntityGraph(attributePaths = "permissions")
    List<Role> findAllByIdIn(List<UUID> ids);

    @EntityGraph(attributePaths = "permissions")
    List<Role> findAll();
}
