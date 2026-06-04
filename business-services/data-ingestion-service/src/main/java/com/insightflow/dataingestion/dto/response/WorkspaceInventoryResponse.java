package com.insightflow.dataingestion.dto.response;

import com.insightflow.dataingestion.entity.InventoryFact;
import com.insightflow.dataingestion.entity.Product;
import com.insightflow.dataingestion.entity.ProductVariant;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WorkspaceInventoryResponse {
    private List<Product> products;
    private List<ProductVariant> variants;
    private List<InventoryFact> inventoryFacts;
}