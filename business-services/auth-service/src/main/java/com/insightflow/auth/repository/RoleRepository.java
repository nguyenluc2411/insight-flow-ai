package com.insightflow.auth.repository;

import com.insightflow.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    List<Role> findByTenantIdIsNullAndNameIn(List<String> names);
}
