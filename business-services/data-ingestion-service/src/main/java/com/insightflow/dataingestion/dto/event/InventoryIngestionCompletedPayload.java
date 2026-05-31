package com.insightflow.dataingestion.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryIngestionCompletedPayload {
    @JsonProperty("workspace_id")
    private String workspaceId;
    @JsonProperty("total_items")
    private Integer totalItems;
    @JsonProperty("completeness_score")
    private Double completenessScore;
    @JsonProperty("missing_fields")
    private List<String> missingFields;
}