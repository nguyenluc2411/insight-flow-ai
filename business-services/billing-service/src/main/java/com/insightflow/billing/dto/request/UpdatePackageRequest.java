package com.insightflow.billing.dto.request;

import lombok.Data;

/** Super-admin: patch package metadata / visibility. Null fields are left unchanged. */
@Data
public class UpdatePackageRequest {
    private String name;
    private String description;
    private Integer displayOrder;
    /** ACTIVE = visible to customers, INACTIVE = hidden. */
    private String status;
}
