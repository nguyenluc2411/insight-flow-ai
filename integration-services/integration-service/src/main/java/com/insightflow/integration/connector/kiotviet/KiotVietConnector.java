package com.insightflow.integration.connector.kiotviet;

import com.insightflow.integration.connector.kiotviet.model.KvBranch;
import com.insightflow.integration.connector.kiotviet.model.KvInventory;
import com.insightflow.integration.connector.kiotviet.model.KvOrder;
import com.insightflow.integration.connector.kiotviet.model.KvProduct;
import com.insightflow.integration.core.ConnectorInterface;
import com.insightflow.integration.core.ConnectorRateLimiter;
import com.insightflow.integration.core.ConnectorType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KiotVietConnector implements ConnectorInterface {

    private final KiotVietAuthClient authClient;
    private final KiotVietProductClient productClient;
    private final KiotVietOrderClient orderClient;
    private final KiotVietInventoryClient inventoryClient;
    private final KiotVietWebhookVerifier webhookVerifier;
    private final ConnectorRateLimiter rateLimiter;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.integration.kiotviet.api-base-url:https://public.kiotapi.com}")
    private String apiBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public ConnectorType getType() {
        return ConnectorType.KIOTVIET;
    }

    @Override
    public boolean authenticate(Map<String, String> credentials) {
        try {
            String clientId = credentials.get("clientId");
            String clientSecret = credentials.get("clientSecret");
            if (clientId == null || clientSecret == null) {
                log.warn("KiotViet authenticate: missing clientId or clientSecret");
                return false;
            }
            authClient.invalidate(clientId);
            String token = authClient.getAccessToken(clientId, clientSecret);
            return token != null && !token.isBlank();
        } catch (Exception e) {
            log.error("KiotViet authentication failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<KvProduct> fetchProducts(int currentItem, int pageSize) {
        throw new UnsupportedOperationException(
                "Use fetchProducts(token, retailer, currentItem, pageSize) instead");
    }

    public List<KvProduct> fetchProducts(String accessToken, String retailerName,
                                          int currentItem, int pageSize) {
        rateLimiter.acquirePermission(ConnectorType.KIOTVIET);
        return productClient.fetchProducts(accessToken, retailerName, currentItem, pageSize);
    }

    @Override
    public List<KvOrder> fetchOrders(Instant fromTime, int currentItem, int pageSize) {
        throw new UnsupportedOperationException(
                "Use fetchOrders(token, retailer, fromTime, currentItem, pageSize) instead");
    }

    public List<KvOrder> fetchOrders(String accessToken, String retailerName,
                                      Instant fromTime, int currentItem, int pageSize) {
        rateLimiter.acquirePermission(ConnectorType.KIOTVIET);
        return orderClient.fetchOrders(accessToken, retailerName, fromTime, currentItem, pageSize);
    }

    @Override
    public List<KvInventory> fetchInventory(Long branchId, int currentItem, int pageSize) {
        throw new UnsupportedOperationException(
                "Use fetchInventory(token, retailer, branchId, currentItem, pageSize) instead");
    }

    public List<KvInventory> fetchInventory(String accessToken, String retailerName,
                                              Long branchId, int currentItem, int pageSize) {
        rateLimiter.acquirePermission(ConnectorType.KIOTVIET);
        return inventoryClient.fetchInventory(accessToken, retailerName, branchId, currentItem, pageSize);
    }

    @Override
    public List<KvBranch> fetchBranches() {
        throw new UnsupportedOperationException(
                "Use fetchBranches(token, retailer) instead");
    }

    public List<KvBranch> fetchBranches(String accessToken, String retailerName) {
        rateLimiter.acquirePermission(ConnectorType.KIOTVIET);
        try {
            JsonNode response = webClientBuilder.build().get()
                    .uri(apiBaseUrl + "/branches")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Retailer", retailerName)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.has("data")) {
                return Collections.emptyList();
            }
            List<KvBranch> branches = new ArrayList<>();
            response.get("data").forEach(node -> {
                try {
                    branches.add(objectMapper.treeToValue(node, KvBranch.class));
                } catch (Exception e) {
                    log.warn("Failed to deserialize KvBranch: {}", e.getMessage());
                }
            });
            return branches;
        } catch (WebClientResponseException e) {
            log.error("KiotViet branches API error: status={}", e.getStatusCode());
            throw new RuntimeException("KiotViet branches fetch failed: " + e.getStatusCode(), e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        return webhookVerifier.verify(payload, signature, secret);
    }
}
