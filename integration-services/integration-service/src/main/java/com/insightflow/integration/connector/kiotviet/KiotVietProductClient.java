package com.insightflow.integration.connector.kiotviet;

import com.fasterxml.jackson.databind.JsonNode;
import com.insightflow.integration.connector.kiotviet.model.KvProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class KiotVietProductClient {

    private final WebClient webClient;

    @Value("${app.integration.kiotviet.api-base-url:https://public.kiotapi.com}")
    private String apiBaseUrl;

    public KiotVietProductClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public List<KvProduct> fetchProducts(String accessToken, String retailerName,
                                          int currentItem, int pageSize) {
        try {
            JsonNode response = webClient.get()
                    .uri(apiBaseUrl + "/products?pageSize={ps}&currentItem={ci}&includeInventory=false",
                            pageSize, currentItem)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Retailer", retailerName)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.has("data")) {
                return Collections.emptyList();
            }

            List<KvProduct> products = new java.util.ArrayList<>();
            response.get("data").forEach(node -> {
                try {
                    KvProduct p = new com.fasterxml.jackson.databind.ObjectMapper()
                            .treeToValue(node, KvProduct.class);
                    products.add(p);
                } catch (Exception e) {
                    log.warn("Failed to deserialize KvProduct: {}", e.getMessage());
                }
            });
            log.debug("Fetched {} products at offset {}", products.size(), currentItem);
            return products;

        } catch (WebClientResponseException e) {
            log.error("KiotViet products API error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("KiotViet products fetch failed: " + e.getStatusCode(), e);
        }
    }
}
