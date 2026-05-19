package com.insightflow.recommendation.mapper;

import com.insightflow.recommendation.dto.request.RecommendationRequest;
import com.insightflow.recommendation.dto.response.RecommendationResponse;
import com.insightflow.recommendation.entity.Recommendation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecommendationMapper {

    RecommendationResponse toResponse(Recommendation recommendation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "recommendationType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "confidenceScore", ignore = true)
    @Mapping(target = "riskLevel", ignore = true)
    @Mapping(target = "recommendationReason", ignore = true)
    @Mapping(target = "actionPayload", ignore = true)
    @Mapping(target = "generatedAt", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Recommendation toEntity(RecommendationRequest request);
}
