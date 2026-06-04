package com.insightflow.bff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MlMarketTrendsResponse {

    private String location;
    private List<TrendItem> trends;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrendItem {
        private String name;
        private String tag;
        private int growthPct;
    }
}
