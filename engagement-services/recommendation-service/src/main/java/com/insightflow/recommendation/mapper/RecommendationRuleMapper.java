package com.insightflow.recommendation.mapper;

import com.insightflow.recommendation.dto.request.RecommendationRuleRequest;
import com.insightflow.recommendation.dto.response.RecommendationRuleResponse;
import com.insightflow.recommendation.entity.RecommendationRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecommendationRuleMapper {

    RecommendationRuleResponse toResponse(RecommendationRule rule);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RecommendationRule toEntity(RecommendationRuleRequest request);
}
