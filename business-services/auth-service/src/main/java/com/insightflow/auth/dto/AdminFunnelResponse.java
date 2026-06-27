package com.insightflow.auth.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AdminFunnelResponse {
    private long totalVisitors;
    private long totalRegistered;
    private long totalPaidCustomers;
    private Map<String, Double> conversionRates;
    private List<FunnelStep> funnelSteps;

    private List<AccessHistoryItem> accessHistory;

    @Data
    @Builder
    public static class FunnelStep {
        private String stepName;
        private long value;
    }

    @Data
    @Builder
    public static class AccessHistoryItem {
        private String id;
        private String visitorType; // "Khách vãng lai" or "User: Name"
        private String plan;
        private String accessTime;
        private String duration;
        private String device;
        private String location;
    }
}
