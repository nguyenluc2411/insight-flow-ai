package com.insightflow.recommendation.mapper;

import com.insightflow.recommendation.dto.response.RecommendationHistoryResponse;
import com.insightflow.recommendation.entity.RecommendationHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecommendationHistoryMapper {

    @Mapping(target = "recommendationId", source = "recommendation.id")
    RecommendationHistoryResponse toResponse(RecommendationHistory history);
}

