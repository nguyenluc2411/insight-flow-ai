package com.insightflow.integration.connector.kiotviet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.integration.connector.kiotviet.model.KvOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class KiotVietOrderClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @Value("${app.integration.kiotviet.api-base-url:https://public.kiotapi.com}")
    private String apiBaseUrl;

    public KiotVietOrderClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public List<KvOrder> fetchOrders(String accessToken, String retailerName,
                                      Instant fromTime, int currentItem, int pageSize) {
        try {
            String fromDateStr = DateTimeFormatter.ISO_INSTANT.format(fromTime);

            JsonNode response = webClient.get()
                    .uri(apiBaseUrl + "/orders?pageSize={ps}&currentItem={ci}" +
                                 "&fromPurchaseDate={from}&orderBy=purchaseDate&orderDirection=ASC",
                            pageSize, currentItem, fromDateStr)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Retailer", retailerName)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.has("data")) {
                return Collections.emptyList();
            }

            List<KvOrder> orders = new ArrayList<>();
            response.get("data").forEach(node -> {
                try {
                    orders.add(objectMapper.treeToValue(node, KvOrder.class));
                } catch (Exception e) {
                    log.warn("Failed to deserialize KvOrder: {}", e.getMessage());
                }
            });
            log.debug("Fetched {} orders at offset {}", orders.size(), currentItem);
            return orders;

        } catch (WebClientResponseException e) {
            log.error("KiotViet orders API error: status={}", e.getStatusCode());
            throw new RuntimeException("KiotViet orders fetch failed: " + e.getStatusCode(), e);
        }
    }
}
