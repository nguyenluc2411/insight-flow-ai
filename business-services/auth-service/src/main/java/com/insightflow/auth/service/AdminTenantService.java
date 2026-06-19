package com.insightflow.auth.service;

import com.insightflow.auth.dto.response.AdminMetricsResponse;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

    /** Aggregate tenant/user metrics for the super-admin dashboard. */
    @Transactional(readOnly = true)
    public AdminMetricsResponse getMetrics(int days) {
        int window = Math.max(1, Math.min(days, 365));

        Map<String, Long> byStatus = toCountMap(tenantRepository.countByStatus(platformSlug));
        Map<String, Long> byPlan = toCountMap(tenantRepository.countByPlan(platformSlug));

        long total = tenantRepository.countNonPlatform(platformSlug);
        long active = byStatus.getOrDefault("active", 0L);
        long trial = byStatus.getOrDefault("trial", 0L);
        // Anything not active/trial (suspended, cancelled, …) counts as suspended.
        long suspended = total - active - trial;

        // Bucket sign-ups by calendar day (UTC), pre-seeding every day in the
        // window with 0 so the chart has a continuous x-axis.
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant since = today.minusDays(window - 1L).atStartOfDay(ZoneOffset.UTC).toInstant();
        Map<LocalDate, Long> seriesMap = new TreeMap<>();
        for (int i = 0; i < window; i++) {
            seriesMap.put(today.minusDays(window - 1L - i), 0L);
        }
        for (Instant createdAt : tenantRepository.findCreatedAtSince(platformSlug, since)) {
            LocalDate day = createdAt.atZone(ZoneOffset.UTC).toLocalDate();
            seriesMap.computeIfPresent(day, (k, v) -> v + 1);
        }
        List<AdminMetricsResponse.DailyCount> series = seriesMap.entrySet().stream()
                .map(e -> new AdminMetricsResponse.DailyCount(e.getKey(), e.getValue()))
                .toList();

        return new AdminMetricsResponse(
                total, active, trial, Math.max(suspended, 0),
                userRepository.count(), byStatus, byPlan, series);
    }

    private Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] == null ? "unknown" : row[0].toString();
            map.put(key, ((Number) row[1]).longValue());
        }
        return map;
    }

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
