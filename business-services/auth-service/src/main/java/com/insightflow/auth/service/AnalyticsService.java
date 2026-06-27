package com.insightflow.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.auth.dto.AdminFunnelResponse;
import com.insightflow.auth.dto.AnalyticsEventRequest;
import com.insightflow.auth.entity.AnalyticsEvent;
import com.insightflow.auth.repository.AnalyticsEventRepository;
import com.insightflow.auth.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AnalyticsEventRepository analyticsEventRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${app.admin.platform-tenant-slug:platform}")
    private String platformSlug;

    @Transactional
    public void trackEvent(AnalyticsEventRequest request) {
        String metadataJson = null;
        if (request.getMetadata() != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(request.getMetadata());
            } catch (Exception e) {
                log.warn("Failed to serialize metadata for session {}", request.getSessionId());
            }
        }

        AnalyticsEvent event = AnalyticsEvent.builder()
                .id(UUID.randomUUID())
                .sessionId(request.getSessionId())
                .tenantId(request.getTenantId())
                .eventType(request.getEventType())
                .url(request.getUrl())
                .utmSource(request.getUtmSource())
                .metadata(metadataJson)
                .createdAt(OffsetDateTime.now())
                .build();

        analyticsEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public AdminFunnelResponse getFunnelAnalytics(java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        OffsetDateTime fromOffset = null;
        java.time.Instant fromInstant = null;
        OffsetDateTime toOffset = null;
        java.time.Instant toInstant = null;

        if (fromDate != null) {
            fromOffset = fromDate.atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime();
            fromInstant = fromDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        }
        if (toDate != null) {
            toOffset = toDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime().minusNanos(1);
            toInstant = toDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().minusNanos(1);
        }

        long totalVisitors = analyticsEventRepository.countUniqueVisitorsSince(fromOffset, toOffset);
        long totalRegistered = tenantRepository.countNonPlatformSince(platformSlug, fromInstant, toInstant);

        // Calculate actual paid customers
        long totalPaidCustomers = tenantRepository.countPaidCustomersSince(platformSlug, fromInstant, toInstant);

        double visitorToRegisterPct = totalVisitors > 0 ? (double) totalRegistered / totalVisitors * 100 : 0.0;
        double registerToPaidPct = totalRegistered > 0 ? (double) totalPaidCustomers / totalRegistered * 100 : 0.0;

        visitorToRegisterPct = Math.min(visitorToRegisterPct, 100.0);
        registerToPaidPct = Math.min(registerToPaidPct, 100.0);

        java.util.List<AnalyticsEventRepository.SessionStats> recentSessions = analyticsEventRepository.findRecentSessionStats(
                fromOffset, toOffset, org.springframework.data.domain.PageRequest.of(0, 50));

        List<AdminFunnelResponse.AccessHistoryItem> accessHistory = new java.util.ArrayList<>();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(java.time.ZoneId.systemDefault());

        for (AnalyticsEventRepository.SessionStats stats : recentSessions) {
            String sessionId = stats.getSessionId();
            
            AnalyticsEvent latestEvent = analyticsEventRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId);
            if (latestEvent == null) continue;

            long durationSeconds = java.time.Duration.between(stats.getFirstAccess(), stats.getLastAccess()).getSeconds();
            String duration = durationSeconds + "s";
            if (durationSeconds > 0) {
                long m = durationSeconds / 60;
                long s = durationSeconds % 60;
                if (m == 0) {
                    duration = s + "s";
                } else if (m < 60) {
                    duration = m + "m " + s + "s";
                } else {
                    long h = m / 60;
                    m = m % 60;
                    duration = h + "h " + m + "m " + s + "s";
                }
            }

            String visitorType = "Khách vãng lai";
            String plan = "Chưa đăng ký";
            UUID tenantId = latestEvent.getTenantId();

            if (tenantId != null) {
                com.insightflow.auth.entity.Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
                if (tenant != null) {
                    visitorType = tenant.getName();
                    plan = tenant.getPlan();
                }
            }

            String device = "-";
            String location = "-";
            if (latestEvent.getMetadata() != null) {
                try {
                    com.fasterxml.jackson.databind.JsonNode metaNode = objectMapper.readTree(latestEvent.getMetadata());
                    if (metaNode.has("device")) device = metaNode.get("device").asText();
                    if (metaNode.has("location")) location = metaNode.get("location").asText();
                } catch (Exception ignored) {}
            }

            accessHistory.add(AdminFunnelResponse.AccessHistoryItem.builder()
                    .id(sessionId.length() > 8 ? sessionId.substring(0, 8) + "..." : sessionId)
                    .visitorType(visitorType)
                    .plan(plan)
                    .accessTime(dtf.format(stats.getLastAccess())) // Use latest activity time
                    .duration(duration)
                    .device(device)
                    .location(location)
                    .build());
        }

        return AdminFunnelResponse.builder()
                .totalVisitors(totalVisitors)
                .totalRegistered(totalRegistered)
                .totalPaidCustomers(totalPaidCustomers)
                .conversionRates(Map.of(
                        "visitorToRegisterPct", Math.round(visitorToRegisterPct * 100.0) / 100.0,
                        "registerToPaidPct", Math.round(registerToPaidPct * 100.0) / 100.0
                ))
                .funnelSteps(List.of(
                        AdminFunnelResponse.FunnelStep.builder().stepName("Visitors").value(totalVisitors).build(),
                        AdminFunnelResponse.FunnelStep.builder().stepName("Registered").value(totalRegistered).build(),
                        AdminFunnelResponse.FunnelStep.builder().stepName("Paid Customers").value(totalPaidCustomers).build()
                ))
                .accessHistory(accessHistory)
                .build();
    }
}
