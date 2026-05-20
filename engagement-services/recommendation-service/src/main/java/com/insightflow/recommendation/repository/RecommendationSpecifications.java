package com.insightflow.recommendation.repository;

import com.insightflow.recommendation.dto.request.RecommendationFilterRequest;
import com.insightflow.recommendation.entity.Recommendation;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public final class RecommendationSpecifications {

    private RecommendationSpecifications() {
    }

    public static Specification<Recommendation> withFilter(RecommendationFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getRecommendationType() != null) {
                predicates.add(cb.equal(root.get("recommendationType"), filter.getRecommendationType()));
            }

            if (filter.getRiskLevel() != null) {
                predicates.add(cb.equal(root.get("riskLevel"), filter.getRiskLevel()));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getProductId() != null) {
                predicates.add(cb.equal(root.get("productId"), filter.getProductId()));
            }

            LocalDate startDate = filter.getStartDate();
            LocalDate endDate = filter.getEndDate();

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("generatedAt"),
                        startDate.atStartOfDay().toInstant(ZoneOffset.UTC)));
            }

            if (endDate != null) {
                predicates.add(cb.lessThan(
                        root.get("generatedAt"),
                        endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

