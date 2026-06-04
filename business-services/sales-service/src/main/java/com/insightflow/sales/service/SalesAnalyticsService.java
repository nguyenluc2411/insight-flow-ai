package com.insightflow.sales.service;

import com.insightflow.sales.dto.response.ChannelStatsItem;
import com.insightflow.sales.dto.response.LocationStatsItem;
import com.insightflow.sales.dto.response.SalesAnalyticsResponse;
import com.insightflow.sales.repository.SalesOrderRepository;
import com.insightflow.sales.repository.projection.ChannelAggregation;
import com.insightflow.sales.repository.projection.LocationAggregation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesAnalyticsService {

    private final SalesOrderRepository orderRepository;

    private static final Map<String, String> CHANNEL_MAP = Map.of(
            "pos",      "OFFLINE",
            "kiotviet", "OFFLINE",
            "sapo",     "OFFLINE",
            "shopee",   "SHOPEE",
            "tiktok",   "TIKTOK",
            "lazada",   "LAZADA",
            "website",  "WEBSITE",
            "haravan",  "WEBSITE"
    );

    public SalesAnalyticsResponse getAnalytics(UUID tenantId, String period) {
        Instant[] current = parsePeriod(period);
        Instant[] previous = previousPeriod(current);

        List<ChannelAggregation> curChannel = orderRepository.aggregateByChannel(tenantId, current[0], current[1]);
        List<ChannelAggregation> prevChannel = orderRepository.aggregateByChannel(tenantId, previous[0], previous[1]);
        List<LocationAggregation> curLocation = orderRepository.aggregateByLocation(tenantId, current[0], current[1]);
        List<LocationAggregation> prevLocation = orderRepository.aggregateByLocation(tenantId, previous[0], previous[1]);

        return SalesAnalyticsResponse.builder()
                .period(period)
                .channelStats(buildChannelStats(curChannel, prevChannel))
                .locationStats(buildLocationStats(curLocation, prevLocation))
                .build();
    }

    private List<ChannelStatsItem> buildChannelStats(List<ChannelAggregation> current,
                                                      List<ChannelAggregation> previous) {
        BigDecimal totalRevenue = current.stream()
                .map(c -> c.getTotalRevenue() != null ? c.getTotalRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> prevMap = new java.util.HashMap<>();
        for (ChannelAggregation p : previous) {
            String norm = normalize(p.getChannel());
            prevMap.merge(norm, p.getTotalRevenue() != null ? p.getTotalRevenue() : BigDecimal.ZERO, BigDecimal::add);
        }

        Map<String, ChannelStatsItem.ChannelStatsItemBuilder> merged = new java.util.LinkedHashMap<>();
        for (ChannelAggregation c : current) {
            String norm = normalize(c.getChannel());
            BigDecimal rev = c.getTotalRevenue() != null ? c.getTotalRevenue() : BigDecimal.ZERO;
            merged.merge(norm,
                    ChannelStatsItem.builder().channel(norm).orderCount(c.getOrderCount()).totalRevenue(rev),
                    (a, b) -> a.orderCount(a.build().getOrderCount() + b.build().getOrderCount())
                               .totalRevenue(a.build().getTotalRevenue().add(b.build().getTotalRevenue())));
        }

        return merged.entrySet().stream().map(e -> {
            String ch = e.getKey();
            ChannelStatsItem item = e.getValue().build();
            BigDecimal rev = item.getTotalRevenue();
            int score = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? rev.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
            BigDecimal prevRev = prevMap.getOrDefault(ch, BigDecimal.ZERO);
            int growth = prevRev.compareTo(BigDecimal.ZERO) > 0
                    ? rev.subtract(prevRev).multiply(BigDecimal.valueOf(100))
                         .divide(prevRev, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
            return ChannelStatsItem.builder()
                    .channel(ch)
                    .orderCount(item.getOrderCount())
                    .totalRevenue(rev)
                    .scorePct(score)
                    .growthPct(growth)
                    .build();
        }).toList();
    }

    private List<LocationStatsItem> buildLocationStats(List<LocationAggregation> current,
                                                        List<LocationAggregation> previous) {
        Map<UUID, Long> prevMap = new java.util.HashMap<>();
        for (LocationAggregation p : previous) {
            if (p.getLocationId() != null) prevMap.put(p.getLocationId(), p.getOrderCount());
        }

        return current.stream()
                .filter(c -> c.getLocationId() != null)
                .map(c -> {
                    long prevCount = prevMap.getOrDefault(c.getLocationId(), 0L);
                    int growth = prevCount > 0
                            ? (int) ((c.getOrderCount() - prevCount) * 100L / prevCount)
                            : 0;
                    return LocationStatsItem.builder()
                            .locationId(c.getLocationId())
                            .orderCount(c.getOrderCount())
                            .growthPct(growth)
                            .build();
                })
                .toList();
    }

    private String normalize(String raw) {
        if (raw == null) return "OFFLINE";
        return CHANNEL_MAP.getOrDefault(raw.toLowerCase(), raw.toUpperCase());
    }

    // "2026-Q2" → [2026-04-01T00:00:00Z, 2026-07-01T00:00:00Z)
    static Instant[] parsePeriod(String period) {
        String[] parts = period.split("-Q");
        int year = Integer.parseInt(parts[0]);
        int q = Integer.parseInt(parts[1]);
        int startMonth = (q - 1) * 3 + 1;
        LocalDate start = LocalDate.of(year, startMonth, 1);
        LocalDate end = start.plusMonths(3);
        return new Instant[]{start.atStartOfDay().toInstant(ZoneOffset.UTC),
                             end.atStartOfDay().toInstant(ZoneOffset.UTC)};
    }

    private Instant[] previousPeriod(Instant[] current) {
        long duration = current[1].toEpochMilli() - current[0].toEpochMilli();
        return new Instant[]{Instant.ofEpochMilli(current[0].toEpochMilli() - duration),
                             current[0]};
    }
}
