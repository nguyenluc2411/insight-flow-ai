package com.insightflow.recommendation.service;

import com.insightflow.recommendation.dto.request.ManualRecommendationTriggerRequest;
import com.insightflow.recommendation.dto.request.RecommendationFilterRequest;
import com.insightflow.recommendation.dto.request.RecommendationRequest;
import com.insightflow.recommendation.dto.response.RecommendationAnalyticsResponse;
import com.insightflow.recommendation.dto.response.RecommendationHistoryResponse;
import com.insightflow.recommendation.dto.response.RecommendationResponse;
import com.insightflow.recommendation.entity.Recommendation;
import com.insightflow.recommendation.enums.RecommendationStatus;
import com.insightflow.recommendation.enums.RecommendationType;
import com.insightflow.recommendation.enums.RiskLevel;
import com.insightflow.recommendation.exception.ResourceNotFoundException;
import com.insightflow.recommendation.mapper.RecommendationMapper;
import com.insightflow.recommendation.mapper.RecommendationHistoryMapper;
import com.insightflow.recommendation.repository.RecommendationRepository;
import com.insightflow.recommendation.repository.RecommendationHistoryRepository;
import com.insightflow.recommendation.repository.RecommendationSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "generatedAt", "createdAt", "updatedAt", "priority", "riskLevel", "status", "recommendationType");

    private final RecommendationRepository recommendationRepository;
    private final RecommendationHistoryRepository recommendationHistoryRepository;
    private final RecommendationMapper recommendationMapper;
    private final RecommendationHistoryMapper recommendationHistoryMapper;
    private final RecommendationRuleEngine ruleEngine;

    @Transactional
    public RecommendationResponse createRecommendation(RecommendationRequest request) {
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
    public RecommendationRuleEngine.RecommendationDecision evaluateInventoryState(RecommendationRequest request) {
        return ruleEngine.evaluate(request);
    }

    @Transactional
    public RecommendationResponse refreshRecommendation(UUID recommendationId, RecommendationRequest request) {
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

    @Transactional(readOnly = true)
    public Page<RecommendationResponse> fetchRecommendations(Pageable pageable) {
        return recommendationRepository.findAll(pageable)
                .map(recommendationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<RecommendationResponse> getRecommendations(RecommendationFilterRequest filter) {
        PageRequest pageable = buildPageRequest(filter);
        return recommendationRepository.findAll(RecommendationSpecifications.withFilter(filter), pageable)
                .map(recommendationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendation(UUID recommendationId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation " + recommendationId + " not found"));
        return recommendationMapper.toResponse(recommendation);
    }

    @Transactional(readOnly = true)
    public Page<RecommendationHistoryResponse> getRecommendationHistory(UUID recommendationId, Pageable pageable) {
        if (!recommendationRepository.existsById(recommendationId)) {
            throw new ResourceNotFoundException("Recommendation " + recommendationId + " not found");
        }
        return recommendationHistoryRepository.findByRecommendationId(recommendationId, pageable)
                .map(recommendationHistoryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RecommendationAnalyticsResponse getRecommendationAnalytics() {
        RecommendationAnalyticsResponse response = new RecommendationAnalyticsResponse();
        response.setTotalRecommendations(recommendationRepository.count());
        response.setClearanceCount(recommendationRepository.countByRecommendationType(RecommendationType.CLEARANCE));
        response.setRestockCount(recommendationRepository.countByRecommendationType(RecommendationType.RESTOCK));
        response.setHighRiskCount(recommendationRepository.countByRiskLevelIn(List.of(RiskLevel.HIGH, RiskLevel.CRITICAL)));
        response.setAverageConfidenceScore(resolveAverageConfidenceScore());
        return response;
    }

    @Transactional
    public RecommendationResponse triggerManualRecommendation(ManualRecommendationTriggerRequest request) {
        if (request.getStockQuantity() == null || request.getSalesVelocity() == null || request.getTrendScore() == null) {
            throw new IllegalArgumentException("Manual trigger requires stockQuantity, salesVelocity, and trendScore");
        }

        RecommendationRequest evaluationRequest = new RecommendationRequest();
        evaluationRequest.setProductId(request.getProductId());
        evaluationRequest.setWarehouseId(request.getWarehouseId());
        evaluationRequest.setEventId(UUID.randomUUID());
        evaluationRequest.setStockQuantity(request.getStockQuantity());
        evaluationRequest.setSalesVelocity(request.getSalesVelocity());
        evaluationRequest.setTrendScore(request.getTrendScore());

        RecommendationRuleEngine.RecommendationDecision decision = ruleEngine.evaluate(evaluationRequest);
        Recommendation recommendation = recommendationMapper.toEntity(evaluationRequest);
        recommendation.setRecommendationType(decision.recommendationType());
        recommendation.setRecommendationReason(decision.reason());
        recommendation.setConfidenceScore(decision.confidenceScore());
        recommendation.setStatus(RecommendationStatus.GENERATED);
        recommendation.setGeneratedAt(Instant.now());
        recommendation.getActionPayload().put("triggerReason", request.getTriggerReason());

        Recommendation saved = recommendationRepository.save(recommendation);
        return recommendationMapper.toResponse(saved);
    }

    private PageRequest buildPageRequest(RecommendationFilterRequest filter) {
        String sortBy = filter.getSortBy();
        if (sortBy == null || sortBy.isBlank() || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "generatedAt";
        }
        Sort.Direction direction = filter.getSortDirection() != null ? filter.getSortDirection() : Sort.Direction.DESC;
        return PageRequest.of(filter.getPage(), filter.getSize(), Sort.by(direction, sortBy));
    }

    private BigDecimal resolveAverageConfidenceScore() {
        BigDecimal average = recommendationRepository.findAverageConfidenceScore();
        return average != null ? average : BigDecimal.ZERO;
    }
}
