package com.insightflow.productcatalog.dto.request;

import lombok.Data;

import java.util.List;

/**
 * Yêu cầu phân giải tên cột: nhận danh sách header thô (đã hoặc chưa normalize)
 * từ một file người dùng upload, trả về ánh xạ header -> trường chuẩn.
 */
@Data
public class ColumnResolveRequest {
    private List<String> headers;
}
