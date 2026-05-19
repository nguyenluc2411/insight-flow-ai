package com.insightflow.bff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** Generic paged response shape returned by catalog-service and sales-service. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagedResponse<T> {

    private List<T> content;
    private PageMeta page;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageMeta {
        private int number;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}
