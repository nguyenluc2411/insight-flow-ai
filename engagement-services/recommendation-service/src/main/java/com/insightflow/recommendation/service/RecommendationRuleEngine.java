package com.insightflow.recommendation.service;

import com.insightflow.recommendation.dto.request.RecommendationRequest;
import com.insightflow.recommendation.enums.RecommendationType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class RecommendationRuleEngine {

    private static final int LOW_STOCK_THRESHOLD = 50;
    private static final int HIGH_STOCK_THRESHOLD = 500;
    private static final double LOW_VELOCITY_THRESHOLD = 2.0;
    private static final double HIGH_VELOCITY_THRESHOLD = 10.0;
    private static final double TREND_RISING_THRESHOLD = 70.0;

    public RecommendationDecision evaluate(RecommendationRequest request) {
        int stock = request.getStockQuantity();
        double velocity = request.getSalesVelocity();
        double trendScore = request.getTrendScore();

        if (stock <= LOW_STOCK_THRESHOLD && velocity >= HIGH_VELOCITY_THRESHOLD) {
            return decision(RecommendationType.RESTOCK, "Low stock with high sales velocity", 92.0);
        }

        if (stock >= HIGH_STOCK_THRESHOLD && velocity <= LOW_VELOCITY_THRESHOLD) {
            return decision(RecommendationType.CLEARANCE, "High stock with low sales velocity", 88.0);
        }

        if (trendScore >= TREND_RISING_THRESHOLD) {
            return decision(RecommendationType.PROMOTE, "Trend rising with healthy inventory", 80.0);
        }

        return decision(RecommendationType.OK, "Inventory and sales are stable", 70.0);
    }

    private RecommendationDecision decision(RecommendationType type, String reason, double confidence) {
        BigDecimal score = BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP);
        return new RecommendationDecision(type, reason, score);
    }

    public record RecommendationDecision(
            RecommendationType recommendationType,
            String reason,
            BigDecimal confidenceScore
    ) {
    }
}
