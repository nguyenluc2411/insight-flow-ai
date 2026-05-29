package com.insightflow.bff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * Handles Spring Page format returned by catalog-service and sales-service.
 * Spring Page serialises as flat JSON:
 *   { "content": [...], "totalElements": N, "totalPages": M, "number": 0, "size": 20 }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagedResponse<T> {

    private List<T> content;

    @JsonProperty("totalElements")
    private Long totalElements;

    @JsonProperty("totalPages")
    private Integer totalPages;

    @JsonProperty("number")
    private Integer number;

    @JsonProperty("size")
    private Integer size;

    /** Null-safe accessor used by aggregation service. */
    public long totalCount() {
        return totalElements != null ? totalElements : 0L;
    }

    public List<T> safeContent() {
        return content != null ? content : Collections.emptyList();
    }
}
