package com.insightflow.integration.core;

import com.insightflow.integration.connector.kiotviet.model.KvBranch;
import com.insightflow.integration.connector.kiotviet.model.KvInventory;
import com.insightflow.integration.connector.kiotviet.model.KvOrder;
import com.insightflow.integration.connector.kiotviet.model.KvProduct;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface ConnectorInterface {

    ConnectorType getType();

    boolean authenticate(Map<String, String> credentials);

    List<KvProduct> fetchProducts(int currentItem, int pageSize);

    List<KvOrder> fetchOrders(Instant fromTime, int currentItem, int pageSize);

    List<KvInventory> fetchInventory(Long branchId, int currentItem, int pageSize);

    List<KvBranch> fetchBranches();

    boolean verifyWebhookSignature(String payload, String signature, String secret);
}
