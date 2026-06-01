package com.insightflow.recommendation.dto.request;

import com.insightflow.recommendation.enums.RecommendationStatus;
import com.insightflow.recommendation.enums.RecommendationType;
import com.insightflow.recommendation.enums.RiskLevel;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class RecommendationFilterRequest {

    private RecommendationType recommendationType;

    private RiskLevel riskLevel;

    private RecommendationStatus status;

    private UUID productId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Min(0)
    private Integer page = 0;

    @Min(1)
    @Max(100)
    private Integer size = 20;

    private String sortBy = "generatedAt";

    private Sort.Direction sortDirection = Sort.Direction.DESC;

    @AssertTrue(message = "startDate must be before or equal to endDate")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.isAfter(endDate);
    }
}

