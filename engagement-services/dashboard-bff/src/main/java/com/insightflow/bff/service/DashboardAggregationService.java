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
import java.util.Comparator;

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
        // Step 1: get active variants from catalog — no dependency on ML recommendations
        PagedResponse<Map<String, Object>> variantsPage = catalogClient.get()
                .uri("/api/v1/catalog/products/variants?size=20")
                .headers(securityHeaders(user))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PagedResponse<Map<String, Object>>>() {})
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("catalog variants unavailable for forecast: {}", ex.getMessage());
                    return Mono.empty();
                })
                .blockOptional(Duration.ofSeconds(12))
                .orElse(null);

        List<Map<String, Object>> variants = variantsPage != null ? variantsPage.safeContent() : Collections.emptyList();

        // Build variantId → sku lookup so TopProduct cards show readable identifiers
        Map<String, String> variantSkuMap = variants.stream()
                .filter(v -> v.get("id") != null && v.get("sku") != null)
                .collect(Collectors.toMap(
                        v -> v.get("id").toString(),
                        v -> v.get("sku").toString(),
                        (a, b) -> a
                ));

        if (variants.isEmpty()) {
            return ForecastSummaryResponse.builder()
                    .categoryTrends(Collections.emptyList())
                    .topProducts(Collections.emptyList())
                    .overallConfidence(0.0)
                    .partial(false)
                    .hasColdStart(false)
                    .message("Chưa có sản phẩm. Hãy thêm sản phẩm để xem dự báo.")
                    .lastUpdated(Instant.now())
                    .build();
        }

        // Step 2: forecast each variant individually — pass SKU for cold-start category hint
        // Use Flux.fromIterable + flatMap for concurrent calls (max 5 parallel)
        List<MlForecastResponse> forecasts = reactor.core.publisher.Flux.fromIterable(variants)
                .flatMap(v -> {
                    Object idObj = v.get("id");
                    Object skuObj = v.get("sku");
                    if (idObj == null) return reactor.core.publisher.Mono.empty();

                    String variantId = idObj.toString();
                    String skuParam = skuObj != null ? "&sku=" + skuObj : "";
                    return mlClient.get()
                            .uri("/api/v1/ml/forecast/" + variantId + "?days=30" + skuParam)
                            .headers(securityHeaders(user))
                            .retrieve()
                            .bodyToMono(MlForecastResponse.class)
                            .timeout(CALL_TIMEOUT)
                            .onErrorResume(ex -> {
                                log.warn("ml forecast failed for variant={}: {}", variantId, ex.getMessage());
                                return reactor.core.publisher.Mono.empty();
                            });
                }, 5)
                .collectList()
                .blockOptional(Duration.ofSeconds(20))
                .orElse(Collections.emptyList());

        // Step 3: build TopProduct list, sort by total forecast desc
        List<ForecastSummaryResponse.TopProduct> topProducts = forecasts.stream()
                .map(f -> {
                    double totalQty = f.getPredictions() == null ? 0.0 :
                            f.getPredictions().stream()
                                    .filter(p -> p.getPredictedQty() != null)
                                    .mapToDouble(MlForecastResponse.ForecastPoint::getPredictedQty)
                                    .sum();
                    String variantKey = f.getVariantId() != null ? f.getVariantId().toString() : "";
                    return ForecastSummaryResponse.TopProduct.builder()
                            .variantId(f.getVariantId())
                            .sku(variantSkuMap.get(variantKey))
                            .forecastDays30(Math.round(totalQty * 10.0) / 10.0)
                            .confidence(f.getConfidence())
                            .build();
                })
                .sorted(Comparator.comparingDouble(
                        (ForecastSummaryResponse.TopProduct p) -> p.getForecastDays30() != null ? p.getForecastDays30() : 0.0
                ).reversed())
                .collect(Collectors.toList());

        boolean hasColdStart = forecasts.stream()
                .anyMatch(f -> "low".equals(f.getConfidence()));

        double avgConfidence = forecasts.stream()
                .mapToDouble(f -> confidenceScore(f.getConfidence()))
                .average()
                .orElse(0.0);

        return ForecastSummaryResponse.builder()
                .categoryTrends(Collections.emptyList())
                .topProducts(topProducts)
                .overallConfidence(Math.round(avgConfidence * 10.0) / 10.0)
                .partial(false)
                .hasColdStart(hasColdStart)
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

    // -------------------------------------------------------------------------
    // Market Summary
    // -------------------------------------------------------------------------

    public MarketSummaryResponse getMarketSummary(UserContext user, String location, String period) {
        String resolvedPeriod = period != null ? period : currentQuarter();
        String resolvedLocation = location != null ? location : "hcmc";

        Mono<SalesAnalyticsResponse> salesMono = salesClient.get()
                .uri(u -> u.path("/api/v1/sales/analytics/summary")
                           .queryParam("period", resolvedPeriod).build())
                .headers(securityHeaders(user))
                .retrieve()
                .bodyToMono(SalesAnalyticsResponse.class)
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("sales analytics unavailable: {}", ex.getMessage());
                    return Mono.empty();
                });

        Mono<MlPagedRecommendationsResponse> restockMono = mlClient.get()
                .uri(u -> u.path("/api/v1/ml/recommendations")
                           .queryParam("action", "RESTOCK").queryParam("size", "50").build())
                .headers(securityHeaders(user))
                .retrieve()
                .bodyToMono(MlPagedRecommendationsResponse.class)
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("ml restock recommendations unavailable: {}", ex.getMessage());
                    return Mono.empty();
                });

        Mono<MlPagedRecommendationsResponse> clearanceMono = mlClient.get()
                .uri(u -> u.path("/api/v1/ml/recommendations")
                           .queryParam("action", "CLEARANCE").queryParam("size", "100").build())
                .headers(securityHeaders(user))
                .retrieve()
                .bodyToMono(MlPagedRecommendationsResponse.class)
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("ml clearance recommendations unavailable: {}", ex.getMessage());
                    return Mono.empty();
                });

        Mono<MlMarketTrendsResponse> trendsMono = mlClient.get()
                .uri(u -> u.path("/api/v1/ml/market-trends")
                           .queryParam("location", resolvedLocation).build())
                .retrieve()
                .bodyToMono(MlMarketTrendsResponse.class)
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(ex -> {
                    log.warn("ml market-trends unavailable: {}", ex.getMessage());
                    return Mono.empty();
                });

        Mono<List<CatalogLocationResponse>> locationsMono = catalogClient.get()
                .uri("/api/v1/catalog/locations")
                .headers(securityHeaders(user))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CatalogLocationResponse>>() {})
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("catalog locations unavailable: {}", ex.getMessage());
                    return Mono.just(List.of());
                });

        Mono<List<CatalogVariantItem>> variantsMono = catalogClient.get()
                .uri(u -> u.path("/api/v1/catalog/products/variants").queryParam("size", "50").build())
                .headers(securityHeaders(user))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PagedResponse<CatalogVariantItem>>() {})
                .map(p -> p.getContent() != null ? p.getContent() : List.<CatalogVariantItem>of())
                .timeout(CALL_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("catalog variants unavailable for market-summary: {}", ex.getMessage());
                    return Mono.just(List.of());
                });

        var results = Mono.zip(
                        salesMono.defaultIfEmpty(new SalesAnalyticsResponse()),
                        restockMono.defaultIfEmpty(new MlPagedRecommendationsResponse()),
                        clearanceMono.defaultIfEmpty(new MlPagedRecommendationsResponse()),
                        trendsMono.defaultIfEmpty(new MlMarketTrendsResponse()),
                        Mono.zip(locationsMono, variantsMono))
                .block(Duration.ofSeconds(35));

        if (results == null) {
            return MarketSummaryResponse.builder()
                    .period(resolvedPeriod).location(resolvedLocation)
                    .partial(true).lastUpdated(Instant.now())
                    .channelOpportunities(List.of()).regionDemand(List.of())
                    .productOpportunities(List.of()).trendHighlights(List.of())
                    .kpis(MarketSummaryResponse.Kpis.builder().build())
                    .build();
        }

        SalesAnalyticsResponse sales           = results.getT1();
        MlPagedRecommendationsResponse restock = results.getT2();
        MlPagedRecommendationsResponse clearance = results.getT3();
        MlMarketTrendsResponse trends           = results.getT4();
        List<CatalogLocationResponse> locations = results.getT5().getT1();
        List<CatalogVariantItem> variants        = results.getT5().getT2();

        Map<java.util.UUID, String> variantNameMap = variants.stream()
                .filter(v -> v.getId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        CatalogVariantItem::getId,
                        v -> v.getProductName() != null ? v.getProductName()
                                : v.getSku() != null ? v.getSku() : "SKU " + v.getId().toString().substring(0, 8),
                        (a, b) -> a));

        boolean partial = trends.getTrends() == null || trends.getTrends().isEmpty();

        // Location UUID → city mapping
        Map<java.util.UUID, String> locCityMap = locations.stream()
                .filter(l -> l.getId() != null && l.getCity() != null)
                .collect(java.util.stream.Collectors.toMap(
                        CatalogLocationResponse::getId,
                        l -> l.getCity().toLowerCase().replace(" ", "_").replace("hồ_chí_minh", "hcmc")
                               .replace("hà_nội", "hanoi").replace("đà_nẵng", "danang"),
                        (a, b) -> a));

        List<MarketSummaryResponse.ChannelOpportunity> channelOpps = buildChannelOpps(sales);
        List<MarketSummaryResponse.RegionDemand> regionDemand = buildRegionDemand(sales, locCityMap);
        List<MarketSummaryResponse.ProductOpportunity> productOpps = buildProductOpps(restock, variantNameMap);
        List<MarketSummaryResponse.TrendHighlight> trendHighlights = buildTrendHighlights(trends);
        MarketSummaryResponse.Kpis kpis = buildKpis(channelOpps, restock, clearance);

        return MarketSummaryResponse.builder()
                .period(resolvedPeriod)
                .location(resolvedLocation)
                .kpis(kpis)
                .channelOpportunities(channelOpps)
                .regionDemand(regionDemand)
                .productOpportunities(productOpps)
                .trendHighlights(trendHighlights)
                .partial(partial)
                .lastUpdated(Instant.now())
                .build();
    }

    private List<MarketSummaryResponse.ChannelOpportunity> buildChannelOpps(SalesAnalyticsResponse sales) {
        if (sales.getChannelStats() == null) return List.of();
        return sales.getChannelStats().stream()
                .map(c -> MarketSummaryResponse.ChannelOpportunity.builder()
                        .channel(c.getChannel())
                        .score(c.getScorePct())
                        .growthPct(c.getGrowthPct())
                        .build())
                .toList();
    }

    private List<MarketSummaryResponse.RegionDemand> buildRegionDemand(
            SalesAnalyticsResponse sales, Map<java.util.UUID, String> locCityMap) {
        if (sales.getLocationStats() == null) return List.of();
        long maxOrders = sales.getLocationStats().stream().mapToLong(l -> l.getOrderCount()).max().orElse(1L);
        return sales.getLocationStats().stream()
                .map(l -> {
                    String region = locCityMap.getOrDefault(l.getLocationId(),
                            l.getLocationId() != null ? l.getLocationId().toString().substring(0, 8) : "unknown");
                    double ratio = maxOrders > 0 ? (double) l.getOrderCount() / maxOrders : 0;
                    String level = ratio >= 0.8 ? "VERY_HIGH" : ratio >= 0.5 ? "HIGH"
                            : ratio >= 0.25 ? "MEDIUM" : "RISING";
                    return MarketSummaryResponse.RegionDemand.builder()
                            .region(region).demandLevel(level).growthPct(l.getGrowthPct())
                            .build();
                })
                .toList();
    }

    private List<MarketSummaryResponse.ProductOpportunity> buildProductOpps(
            MlPagedRecommendationsResponse restock,
            Map<java.util.UUID, String> variantNameMap) {
        if (restock.getItems() == null) return List.of();
        return restock.getItems().stream()
                .limit(6)
                .map(item -> {
                    String badge = "HIGH".equalsIgnoreCase(item.getPriority()) ? "HOT" : "RECOMMEND_IMPORT";
                    int trendPct = item.getSalesVelocity30d() != null
                            ? (int) Math.min(99, item.getSalesVelocity30d() * 10) : 0;
                    String name = item.getVariantId() != null && variantNameMap.containsKey(item.getVariantId())
                            ? variantNameMap.get(item.getVariantId())
                            : item.getSku() != null ? item.getSku()
                            : item.getVariantId() != null ? "SKU-" + item.getVariantId().toString().substring(0, 8).toUpperCase()
                            : "Sản phẩm";
                    return MarketSummaryResponse.ProductOpportunity.builder()
                            .name(name)
                            .category(item.getCategoryKey() != null ? item.getCategoryKey() : "—")
                            .badge(badge)
                            .trendPct(trendPct)
                            .insight(item.getReason())
                            .imageUrl(null)
                            .build();
                })
                .toList();
    }

    private List<MarketSummaryResponse.TrendHighlight> buildTrendHighlights(MlMarketTrendsResponse trends) {
        if (trends.getTrends() == null) return List.of();
        return trends.getTrends().stream()
                .map(t -> MarketSummaryResponse.TrendHighlight.builder()
                        .name(t.getName()).tag(t.getTag()).growthPct(t.getGrowthPct())
                        .build())
                .toList();
    }

    private MarketSummaryResponse.Kpis buildKpis(
            List<MarketSummaryResponse.ChannelOpportunity> channels,
            MlPagedRecommendationsResponse restock,
            MlPagedRecommendationsResponse clearance) {

        // Best channel by score
        String bestChannel = channels.stream()
                .max(Comparator.comparingInt(MarketSummaryResponse.ChannelOpportunity::getScore))
                .map(MarketSummaryResponse.ChannelOpportunity::getChannel).orElse(null);
        int bestScore = channels.stream()
                .mapToInt(MarketSummaryResponse.ChannelOpportunity::getScore).max().orElse(0);

        // Opportunity score: high-priority restock / total restock * 100, capped 100
        long highRestock = restock.getItems() == null ? 0L : restock.getItems().stream()
                .filter(i -> "HIGH".equalsIgnoreCase(i.getPriority())).count();
        long totalRestock = Math.max(restock.getTotal(), highRestock);
        int opportunityScore = (int) Math.min(100, highRestock * 100L / Math.max(1, totalRestock));

        // Potential product groups: distinct categories in restock recs
        long potentialGroups = restock.getItems() == null ? 0L : restock.getItems().stream()
                .map(MlRecommendationItem::getCategoryKey).filter(Objects::nonNull).distinct().count();

        // Competition level: clearance ratio
        long clearanceTotal = clearance.getTotal();
        long combined = totalRestock + clearanceTotal;
        String compLevel = combined == 0 ? "LOW"
                : clearanceTotal * 100L / combined > 30 ? "HIGH"
                : clearanceTotal * 100L / combined > 15 ? "MEDIUM" : "LOW";

        List<String> hotCategories = clearance.getItems() == null ? List.of()
                : clearance.getItems().stream()
                        .map(MlRecommendationItem::getCategoryKey).filter(Objects::nonNull)
                        .distinct().limit(3).toList();

        return MarketSummaryResponse.Kpis.builder()
                .opportunityScore(opportunityScore)
                .opportunityScoreDelta(0)
                .potentialProductGroups((int) potentialGroups)
                .bestChannel(bestChannel)
                .bestChannelScorePct(bestScore)
                .competitionLevel(compLevel)
                .competitionHotCategories(hotCategories)
                .build();
    }

    private static String currentQuarter() {
        java.time.LocalDate now = java.time.LocalDate.now();
        int q = (now.getMonthValue() - 1) / 3 + 1;
        return now.getYear() + "-Q" + q;
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
