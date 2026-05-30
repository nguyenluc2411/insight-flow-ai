package com.insightflow.auth.repository;

import com.insightflow.auth.entity.UserRole;
import com.insightflow.auth.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findAllByUserIdAndTenantId(UUID userId, UUID tenantId);

    @Query(value = """
            SELECT r.name
            FROM   auth_db.user_roles ur
            JOIN   auth_db.roles r ON ur.role_id = r.id
            WHERE  ur.user_id = :userId
            AND    ur.tenant_id = :tenantId
            """, nativeQuery = true)
    List<String> findRoleNamesByUserIdAndTenantId(
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId);

    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.userId = :userId AND ur.tenantId = :tenantId")
    void deleteByUserIdAndTenantId(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);
}
