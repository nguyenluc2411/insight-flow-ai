package com.insightflow.integration.connector.kiotviet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.common.web.exception.BusinessException;
import com.insightflow.common.web.exception.ErrorCode;
import com.insightflow.integration.connector.kiotviet.model.KvInventory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class KiotVietInventoryClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Value("${app.integration.kiotviet.api-base-url:https://public.kiotapi.com}")
    private String apiBaseUrl;

    public KiotVietInventoryClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public List<KvInventory> fetchInventory(String accessToken, String retailerName,
                                             Long branchId, int currentItem, int pageSize) {
        try {
            String uri = apiBaseUrl + "/inventories?pageSize=" + pageSize +
                         "&currentItem=" + currentItem +
                         (branchId != null ? "&branchIds=" + branchId : "");

            JsonNode response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Retailer", retailerName)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.has("data")) {
                return Collections.emptyList();
            }

            List<KvInventory> items = new ArrayList<>();
            response.get("data").forEach(node -> {
                try {
                    items.add(objectMapper.treeToValue(node, KvInventory.class));
                } catch (Exception e) {
                    log.warn("Failed to deserialize KvInventory: {}", e.getMessage());
                }
            });
            log.debug("Fetched {} inventory records at offset {}", items.size(), currentItem);
            return items;

        } catch (WebClientResponseException e) {
            log.error("KiotViet inventory API error: status={}", e.getStatusCode());
            throw new BusinessException(ErrorCode.DOWNSTREAM_ERROR,
                    "KiotViet inventory fetch failed: " + e.getStatusCode());
        }
    }
}
