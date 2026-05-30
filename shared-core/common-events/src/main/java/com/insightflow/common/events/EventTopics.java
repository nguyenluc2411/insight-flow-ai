package com.insightflow.common.events;

/**
 * Kafka topic name constants.
 * Naming convention: {@code {domain}.{entity}.{action}}
 *
 * <p>Services must use these constants when declaring {@code @KafkaListener(topics = ...)}
 * and when calling {@code KafkaTemplate.send(topic, event)} to prevent topic name drift.
 */
public final class EventTopics {

    // catalog-service → ml-service, dashboard-bff
    public static final String CATALOG_PRODUCT_CREATED    = "catalog.product.created";

    // catalog-service → ml-service, notification-service
    public static final String CATALOG_INVENTORY_UPDATED  = "catalog.inventory.updated";

    // sales-service → catalog-service, ml-service
    public static final String SALES_ORDER_COMPLETED      = "sales.order.completed";

    // integration-service → catalog-service, sales-service
    public static final String INTEGRATION_SYNC_COMPLETED = "integration.sync.completed";

    // ml-service → dashboard-bff, notification-service
    public static final String ML_FORECAST_GENERATED      = "ml.forecast.generated";

    // ml-service → dashboard-bff, notification-service
    public static final String ML_RECOMMENDATION_CREATED  = "ml.recommendation.created";

    private EventTopics() {}
}
