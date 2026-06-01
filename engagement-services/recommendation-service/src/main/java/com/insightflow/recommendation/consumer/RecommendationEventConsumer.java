package com.insightflow.recommendation.consumer;

import com.insightflow.recommendation.event.DemandForecastEvent;
import com.insightflow.recommendation.event.InventoryRiskDetectedEvent;
import com.insightflow.recommendation.event.SalesAnalyticsEvent;
import com.insightflow.recommendation.event.TrendDetectedEvent;
import com.insightflow.recommendation.service.RecommendationEventAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationEventConsumer {

    private final RecommendationEventAuditService auditService;

    @KafkaListener(
            topics = TrendDetectedEvent.TOPIC,
            containerFactory = "trendDetectedKafkaListenerContainerFactory")
    public void onTrendDetected(TrendDetectedEvent event) {
        handleEvent(event.eventId(), event.eventType(), "CONSUME_TREND_DETECTED", Map.of(
                "productId", event.productId(),
                "trendScore", event.trendScore(),
                "trendDirection", event.trendDirection(),
                "region", event.region()
        ));
    }

    @KafkaListener(
            topics = InventoryRiskDetectedEvent.TOPIC,
            containerFactory = "inventoryRiskKafkaListenerContainerFactory")
    public void onInventoryRiskDetected(InventoryRiskDetectedEvent event) {
        handleEvent(event.eventId(), event.eventType(), "CONSUME_INVENTORY_RISK", Map.of(
                "productId", event.productId(),
                "warehouseId", event.warehouseId(),
                "inventoryLevel", event.inventoryLevel(),
                "riskLevel", event.riskLevel(),
                "salesVelocity", event.salesVelocity()
        ));
    }

    @KafkaListener(
            topics = SalesAnalyticsEvent.TOPIC,
            containerFactory = "salesAnalyticsKafkaListenerContainerFactory")
    public void onSalesAnalytics(SalesAnalyticsEvent event) {
        handleEvent(event.eventId(), event.eventType(), "CONSUME_SALES_ANALYTICS", Map.of(
                "productId", event.productId(),
                "dailySales", event.dailySales(),
                "salesVelocity", event.salesVelocity(),
                "weeklyGrowthRate", event.weeklyGrowthRate()
        ));
    }

    @KafkaListener(
            topics = DemandForecastEvent.TOPIC,
            containerFactory = "demandForecastKafkaListenerContainerFactory")
    public void onDemandForecast(DemandForecastEvent event) {
        handleEvent(event.eventId(), event.eventType(), "CONSUME_DEMAND_FORECAST", Map.of(
                "productId", event.productId(),
                "forecastDemand", event.forecastDemand(),
                "forecastConfidence", event.forecastConfidence(),
                "forecastPeriodDays", event.forecastPeriodDays()
        ));
    }

    private void handleEvent(UUID eventId, String eventType, String actionType, Map<String, Object> payload) {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId is required");
        }

        if (auditService.isProcessed(eventId)) {
            log.debug("Skipping duplicate eventId={} eventType={}", eventId, eventType);
            return;
        }

        auditService.recordSuccess(eventId, eventType, actionType, payload);
        log.info("Consumed eventId={} eventType={}", eventId, eventType);
    }
}
