package com.insightflow.auth.service;

import com.insightflow.auth.dto.response.AdminTenantDetail;
import com.insightflow.auth.dto.response.AdminTenantListItem;
import com.insightflow.auth.dto.response.AdminUserItem;
import com.insightflow.auth.entity.Role;
import com.insightflow.auth.entity.Tenant;
import com.insightflow.auth.repository.TenantRepository;
import com.insightflow.auth.repository.UserRepository;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Platform super-admin operations over tenants. The internal "platform" tenant
 * (which hosts the super-admin account) is always excluded from results.
 */
@Service
@RequiredArgsConstructor
public class AdminTenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @Value("${app.admin.platform-tenant-slug:platform}")
    private String platformSlug;

    @Transactional(readOnly = true)
    public Page<AdminTenantListItem> listTenants(String status, String q, Pageable pageable) {
        String statusFilter = (status == null || status.isBlank()) ? null : status.strip();
        String queryFilter = (q == null || q.isBlank()) ? null : q.strip();
        return tenantRepository.searchTenants(platformSlug, statusFilter, queryFilter, pageable)
                .map(t -> new AdminTenantListItem(
                        t.getId(), t.getName(), t.getSlug(), t.getPlan(), t.getStatus(),
                        userRepository.countByTenantId(t.getId()), t.getTrialEndsAt(), t.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public AdminTenantDetail getTenant(UUID id) {
        Tenant t = loadNonPlatformTenant(id);
        List<AdminUserItem> users = userRepository.findByTenantId(id).stream()
                .map(u -> new AdminUserItem(
                        u.getId(), u.getEmail(), u.getFullName(), u.getStatus(),
                        u.getRoles().stream().map(Role::getName).toList(),
                        u.getLastLoginAt(), u.getCreatedAt()))
                .toList();
        return new AdminTenantDetail(
                t.getId(), t.getName(), t.getSlug(), t.getPlan(), t.getStatus(),
                t.getTrialEndsAt(), t.getCreatedAt(), t.getSettings(), users);
    }

    @Transactional
    public AdminTenantDetail updateStatus(UUID id, String status) {
        Tenant t = loadNonPlatformTenant(id);
        t.setStatus(status);
        tenantRepository.save(t);
        return getTenant(id);
    }

    private Tenant loadNonPlatformTenant(UUID id) {
        return tenantRepository.findById(id)
                .filter(t -> !t.getSlug().equals(platformSlug))
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
    }
}
