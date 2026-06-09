package com.insightflow.dataingestion.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Gửi danh sách header (đã normalize bởi parser) sang product-catalog-service để
 * phân giải về trường chuẩn. Khớp body của {@code POST /api/v1/catalog/resolve-columns}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ColumnResolveRequest {
    private List<String> headers;
}
