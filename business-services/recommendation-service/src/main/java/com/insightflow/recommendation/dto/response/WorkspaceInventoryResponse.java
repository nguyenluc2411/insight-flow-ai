package com.insightflow.recommendation.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class WorkspaceInventoryResponse {
    // Đã đồng bộ sử dụng đúng tên Class có hậu tố "Response" mà bạng đã đổi lúc trước
    private List<ProductResponse> products;
    private List<ProductVariantResponse> variants;
    private List<InventoryFactResponse> inventoryFacts;
}