package com.insightflow.bff.service;

import com.insightflow.bff.dto.downstream.*;
import com.insightflow.bff.dto.response.*;
import com.insightflow.security.InternalHeaders;
import com.insightflow.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardAggregationService {

    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(12);

    private final @Qualifier("catalogClient") WebClient catalogClient;
    private final @Qualifier("salesClient") WebClient salesClient;
    private final @Qualifier("mlClient") WebClient mlClient;

    // -------------------------------------------------------------------------
    // Overview
    // -------------------------------------------------------------------------

    public DashboardOverviewResponse getOverview(UserContext user) {
        Mono<Long> totalSKU = catalogClient.get()
                .uri("/api/v1/catalog/products?size=1")
                .headers(securityHeaders(user))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PagedResponse<Map<String, Object>>>() {})
                .map(PagedResponse::totalCount)
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("catalog-service unavailable for overview: {}", ex.getMessage());
                    return Mono.empty();
                });

        Mono<long[]> orderStats = salesClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/sales/orders")
                        .queryParam("status", "completed")
                        .queryParam("today", "true")
                        .queryParam("size", "200")
                        .build())
                .headers(securityHeaders(user))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PagedResponse<SalesOrderItem>>() {})
                .map(p -> {
                    long count = p.totalCount();
                    long revenue = p.getContent() == null ? 0L :
                            p.getContent().stream()
                                    .filter(o -> o.getTotalAmount() != null)
                                    .mapToLong(o -> o.getTotalAmount().longValue())
                                    .sum();
                    return new long[]{count, revenue};
                })
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("sales-service unavailable for overview: {}", ex.getMessage());
                    return Mono.empty();
                });

        Mono<Long> highPriorityAlerts = mlClient.get()
                .uri("/api/v1/ml/recommendations?priority=HIGH&size=1")
                .headers(securityHeaders(user))
                .retrieve()
                .bodyToMono(MlPagedRecommendationsResponse.class)
                .map(r -> r.getTotal())
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("ml-service recommendations unavailable: {}", ex.getMessage());
                    return Mono.empty();
                });

        Mono<String> mlStatus = mlClient.get()
                .uri("/api/v1/ml/health")
                .retrieve()
                .bodyToMono(MlHealthResponse.class)
                .map(h -> h.getStatus() != null ? h.getStatus() : "UNKNOWN")
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("ml-service health unavailable: {}", ex.getMessage());
                    return Mono.just("DOWN");
                });

        // Run all 4 calls in parallel; collect results allowing individual nulls
        Long[] totalSKUResult = {null};
        long[] orderStatsResult = {0L, 0L};
        Long[] alertsResult = {null};
        String[] mlStatusResult = {"UNKNOWN"};
        boolean[] anyPartial = {false};

        Mono.zip(
                totalSKU.defaultIfEmpty(-1L),
                orderStats.defaultIfEmpty(new long[]{-1L, -1L}),
                highPriorityAlerts.defaultIfEmpty(-1L),
                mlStatus
        ).blockOptional(Duration.ofSeconds(15)).ifPresentOrElse(
                tuple -> {
                    long sku = tuple.getT1();
                    long[] stats = tuple.getT2();
                    long alerts = tuple.getT3();

                    if (sku >= 0) totalSKUResult[0] = sku; else anyPartial[0] = true;
                    if (stats[0] >= 0) { orderStatsResult[0] = stats[0]; orderStatsResult[1] = stats[1]; } else anyPartial[0] = true;
                    if (alerts >= 0) alertsResult[0] = alerts; else anyPartial[0] = true;
                    mlStatusResult[0] = tuple.getT4();
                },
                () -> anyPartial[0] = true
        );

        return DashboardOverviewResponse.builder()
                .totalSKU(totalSKUResult[0])
                .ordersToday(orderStatsResult[0] > 0 ? orderStatsResult[0] : null)
                .revenueToday(orderStatsResult[1] > 0 ? orderStatsResult[1] : null)
                .highPriorityAlerts(alertsResult[0])
                .mlStatus(mlStatusResult[0])
                .partial(anyPartial[0])
                .lastUpdated(Instant.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // Health Summary
    // -------------------------------------------------------------------------

    public HealthSummaryResponse getHealthSummary(UserContext user) {
        Mono<PagedResponse<CatalogProductItem>> products = catalogClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/catalog/products").queryParam("size", "100").build())
                .headers(securityHeaders(user))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PagedResponse<CatalogProductItem>>() {})
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("catalog products unavailable for health summary: {}", ex.getMessage());
                    return Mono.empty();
                });

        Mono<MlPagedRecommendationsResponse> slowMoving = mlClient.get()
                .uri("/api/v1/ml/recommendations?action=CLEARANCE&size=200")
                .headers(securityHeaders(user))
                .retrieve()
                .bodyToMono(MlPagedRecommendationsResponse.class)
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("ml clearance recommendations unavailable: {}", ex.getMessage());
                    return Mono.empty();
                });

        boolean[] partial = {false};
        PagedResponse<CatalogProductItem>[] prodResult = new PagedResponse[1];
        MlPagedRecommendationsResponse[] slowResult = new MlPagedRecommendationsResponse[1];

        Mono.zip(
                products.defaultIfEmpty(new PagedResponse<>()),
                slowMoving.defaultIfEmpty(new MlPagedRecommendationsResponse())
        ).blockOptional(Duration.ofSeconds(15)).ifPresentOrElse(
                t -> {
                    prodResult[0] = t.getT1();
                    slowResult[0] = t.getT2();
                },
                () -> partial[0] = true
        );

        long totalProducts = prodResult[0] != null ? prodResult[0].totalCount() : 0L;
        long slowMovingCount = slowResult[0] != null ? slowResult[0].getTotal() : 0L;
        double pressurePct = totalProducts > 0 ? (slowMovingCount * 100.0 / totalProducts) : 0.0;

        // Build category risks from clearance recommendations grouped by action/priority
        List<MlRecommendationItem> clearanceItems = slowResult[0] != null && slowResult[0].getItems() != null
                ? slowResult[0].getItems() : Collections.emptyList();

        List<HealthSummaryResponse.CategoryRisk> categoryRisks = List.of(
                HealthSummaryResponse.CategoryRisk.builder()
                        .category("ALL")
                        .units((long) clearanceItems.size())
                        .riskLevel(pressurePct > 30 ? "HIGH" : pressurePct > 15 ? "MEDIUM" : "LOW")
                        .build()
        );

        return HealthSummaryResponse.builder()
                .inventoryPressurePct(Math.round(pressurePct * 10.0) / 10.0)
                .slowMovingSKUCount(slowMovingCount)
                .categoryRisks(categoryRisks)
                .channelPerformance(Collections.emptyList())
                .partial(partial[0])
                .lastUpdated(Instant.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // Recommendations Summary
    // -------------------------------------------------------------------------

    public RecommendationsSummaryResponse getRecommendationsSummary(UserContext user) {
        Mono<MlPagedRecommendationsResponse> highPriorityRecs = mlClient.get()
                .uri("/api/v1/ml/recommendations?priority=HIGH&size=3")
                .headers(securityHeaders(user))
                .retrieve()
                .bodyToMono(MlPagedRecommendationsResponse.class)
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("ml recommendations unavailable: {}", ex.getMessage());
                    return Mono.empty();
                });

        Mono<MlPagedRecommendationsResponse> allRecs = mlClient.get()
                .uri("/api/v1/ml/recommendations?size=200")
                .headers(securityHeaders(user))
                .retrieve()
                .bodyToMono(MlPagedRecommendationsResponse.class)
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("ml all recommendations unavailable: {}", ex.getMessage());
                    return Mono.empty();
                });

        boolean[] partial = {false};
        MlPagedRecommendationsResponse[] topResult = new MlPagedRecommendationsResponse[1];
        MlPagedRecommendationsResponse[] allResult = new MlPagedRecommendationsResponse[1];

        Mono.zip(
                highPriorityRecs.defaultIfEmpty(new MlPagedRecommendationsResponse()),
                allRecs.defaultIfEmpty(new MlPagedRecommendationsResponse())
        ).blockOptional(Duration.ofSeconds(15)).ifPresentOrElse(
                t -> { topResult[0] = t.getT1(); allResult[0] = t.getT2(); },
                () -> partial[0] = true
        );

        List<MlRecommendationItem> allItems = allResult[0] != null && allResult[0].getItems() != null
                ? allResult[0].getItems() : Collections.emptyList();

        Map<String, Long> byAction = allItems.stream()
                .collect(Collectors.groupingBy(MlRecommendationItem::getAction, Collectors.counting()));

        List<MlRecommendationItem> topItems = topResult[0] != null && topResult[0].getItems() != null
                ? topResult[0].getItems() : Collections.emptyList();

        List<RecommendationsSummaryResponse.TopAction> topActions = topItems.stream()
                .map(r -> RecommendationsSummaryResponse.TopAction.builder()
                        .variantId(r.getVariantId())
                        .action(r.getAction())
                        .priority(r.getPriority())
                        .reason(r.getReason())
                        .suggestedDiscountPct(r.getSuggestedDiscountPct())
                        .suggestedRestockQty(r.getSuggestedRestockQty())
                        .stockAgeDays(r.getStockAgeDays())
                        .currentStock(r.getCurrentStock())
                        .build())
                .collect(Collectors.toList());

        long clearanceCount = byAction.getOrDefault("CLEARANCE", 0L);
        long restockCount = byAction.getOrDefault("RESTOCK", 0L);
        long promoteCount = byAction.getOrDefault("PROMOTE", 0L);

        OptionalDouble avgDiscount = allItems.stream()
                .filter(r -> "CLEARANCE".equals(r.getAction()) && r.getSuggestedDiscountPct() != null)
                .mapToDouble(MlRecommendationItem::getSuggestedDiscountPct)
                .average();

        RecommendationsSummaryResponse.EstimatedImpact impact = RecommendationsSummaryResponse.EstimatedImpact.builder()
                .clearanceItems(clearanceCount)
                .restockItems(restockCount)
                .promoteItems(promoteCount)
                .avgDiscountPct(avgDiscount.isPresent() ? Math.round(avgDiscount.getAsDouble() * 10.0) / 10.0 : null)
                .build();

        long total = allResult[0] != null ? allResult[0].getTotal() : 0L;

        return RecommendationsSummaryResponse.builder()
                .total(total)
                .byAction(byAction)
                .topActions(topActions)
                .estimatedImpact(impact)
                .partial(partial[0])
                .lastUpdated(Instant.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // Forecast Summary
    // -------------------------------------------------------------------------

    public ForecastSummaryResponse getForecastSummary(UserContext user) {
        // Step 1: get top 5 variant IDs from recommendations
        MlPagedRecommendationsResponse recsResult = mlClient.get()
                .uri("/api/v1/ml/recommendations?size=5")
                .headers(securityHeaders(user))
                .retrieve()
                .bodyToMono(MlPagedRecommendationsResponse.class)
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("ml recommendations for forecast unavailable: {}", ex.getMessage());
                    return Mono.empty();
                })
                .blockOptional(Duration.ofSeconds(12))
                .orElse(null);

        if (recsResult == null || recsResult.getItems() == null || recsResult.getItems().isEmpty()) {
            return ForecastSummaryResponse.builder()
                    .categoryTrends(Collections.emptyList())
                    .topProducts(Collections.emptyList())
                    .overallConfidence(0.0)
                    .partial(true)
                    .lastUpdated(Instant.now())
                    .build();
        }

        List<UUID> variantIds = recsResult.getItems().stream()
                .map(MlRecommendationItem::getVariantId)
                .filter(Objects::nonNull)
                .distinct()
                .limit(5)
                .collect(Collectors.toList());

        // Step 2: batch forecast for those variants
        Map<String, Object> batchRequest = Map.of(
                "variantIds", variantIds,
                "days", 30
        );

        List<MlForecastResponse> forecasts = mlClient.post()
                .uri("/api/v1/ml/forecast/batch")
                .headers(securityHeaders(user))
                .header("Content-Type", "application/json")
                .bodyValue(batchRequest)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<MlForecastResponse>>() {})
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("ml batch forecast unavailable: {}", ex.getMessage());
                    return Mono.empty();
                })
                .blockOptional(Duration.ofSeconds(12))
                .orElse(Collections.emptyList());

        List<ForecastSummaryResponse.TopProduct> topProducts = forecasts.stream()
                .map(f -> {
                    double totalQty = f.getPredictions() == null ? 0.0 :
                            f.getPredictions().stream()
                                    .filter(p -> p.getPredictedQty() != null)
                                    .mapToDouble(MlForecastResponse.ForecastPoint::getPredictedQty)
                                    .sum();
                    return ForecastSummaryResponse.TopProduct.builder()
                            .variantId(f.getVariantId())
                            .forecastDays30(Math.round(totalQty * 10.0) / 10.0)
                            .confidence(f.getConfidence())
                            .build();
                })
                .collect(Collectors.toList());

        // Overall confidence: map string confidence to numeric, compute average
        double avgConfidence = forecasts.stream()
                .mapToDouble(f -> confidenceScore(f.getConfidence()))
                .average()
                .orElse(0.0);

        return ForecastSummaryResponse.builder()
                .categoryTrends(Collections.emptyList())
                .topProducts(topProducts)
                .overallConfidence(Math.round(avgConfidence * 10.0) / 10.0)
                .partial(false)
                .lastUpdated(Instant.now())
                .build();
    }

    private double confidenceScore(String confidence) {
        if (confidence == null) return 0.0;
        return switch (confidence.toLowerCase()) {
            case "high" -> 1.0;
            case "medium" -> 0.6;
            case "low" -> 0.3;
            default -> 0.0;
        };
    }

    /**
     * Builds a header consumer that propagates all gateway-injected security headers
     * from the caller's UserContext to downstream service calls.
     * Ensures @RequiresPermission checks pass without re-routing through the gateway.
     */
    private Consumer<HttpHeaders> securityHeaders(UserContext user) {
        return h -> {
            if (user.userId() != null)    h.set(InternalHeaders.X_USER_ID,          user.userId().toString());
            if (user.tenantId() != null)  h.set(InternalHeaders.X_TENANT_ID,        user.tenantId().toString());
            if (user.tenantSlug() != null) h.set(InternalHeaders.X_TENANT_SLUG,     user.tenantSlug());
            if (user.plan() != null)      h.set(InternalHeaders.X_TENANT_PLAN,      user.plan());
            if (!user.roles().isEmpty())  h.set(InternalHeaders.X_USER_ROLES,       String.join(",", user.roles()));
            if (!user.permissions().isEmpty()) h.set(InternalHeaders.X_USER_PERMISSIONS, String.join(",", user.permissions()));
        };
    }
}
