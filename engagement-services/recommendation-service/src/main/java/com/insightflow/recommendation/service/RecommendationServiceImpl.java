package com.insightflow.recommendation.service;

import com.insightflow.recommendation.dto.request.RecommendationRequest;
import com.insightflow.recommendation.dto.response.RecommendationResponse;
import com.insightflow.recommendation.entity.Recommendation;
import com.insightflow.recommendation.enums.RecommendationStatus;
import com.insightflow.recommendation.exception.ResourceNotFoundException;
import com.insightflow.recommendation.mapper.RecommendationMapper;
import com.insightflow.recommendation.repository.RecommendationRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Validated
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final RecommendationMapper recommendationMapper;
    private final RecommendationRuleEngine ruleEngine;

    @Transactional
    public RecommendationResponse createRecommendation(@Valid RecommendationRequest request) {
        RecommendationRuleEngine.RecommendationDecision decision = ruleEngine.evaluate(request);

        Recommendation recommendation = recommendationMapper.toEntity(request);
        recommendation.setRecommendationType(decision.recommendationType());
        recommendation.setRecommendationReason(decision.reason());
        recommendation.setConfidenceScore(decision.confidenceScore());
        recommendation.setStatus(RecommendationStatus.GENERATED);
        recommendation.setGeneratedAt(Instant.now());

        Recommendation saved = recommendationRepository.save(recommendation);
        return recommendationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public RecommendationRuleEngine.RecommendationDecision evaluateInventoryState(@Valid RecommendationRequest request) {
        return ruleEngine.evaluate(request);
    }

    @Transactional(readOnly = true)
    public Page<RecommendationResponse> fetchRecommendations(Pageable pageable) {
        return recommendationRepository.findAll(pageable)
                .map(recommendationMapper::toResponse);
    }

    @Transactional
    public RecommendationResponse refreshRecommendation(UUID recommendationId, @Valid RecommendationRequest request) {
        Recommendation existing = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation " + recommendationId + " not found"));

        RecommendationRuleEngine.RecommendationDecision decision = ruleEngine.evaluate(request);
        existing.setRecommendationType(decision.recommendationType());
        existing.setRecommendationReason(decision.reason());
        existing.setConfidenceScore(decision.confidenceScore());
        existing.setStatus(RecommendationStatus.GENERATED);
        existing.setGeneratedAt(Instant.now());
        existing.setEventId(request.getEventId());
        existing.setProductId(request.getProductId());
        existing.setWarehouseId(request.getWarehouseId());

        Recommendation saved = recommendationRepository.save(existing);
        return recommendationMapper.toResponse(saved);
    }
}
