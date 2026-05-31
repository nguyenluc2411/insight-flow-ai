package com.insightflow.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PackageResponse {

    private UUID id;
    private String code;
    private Integer version;
    private String name;
    private String description;
    private Integer displayOrder;
    private String status;
    private List<PlanResponse> plans;
    private List<String> featureCodes;
}
