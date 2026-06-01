package com.insightflow.recommendation.controller;

import com.insightflow.recommendation.dto.request.ManualRecommendationTriggerRequest;
import com.insightflow.recommendation.dto.request.RecommendationFilterRequest;
import com.insightflow.recommendation.dto.response.ApiResponse;
import com.insightflow.recommendation.dto.response.PageResponse;
import com.insightflow.recommendation.dto.response.RecommendationAnalyticsResponse;
import com.insightflow.recommendation.dto.response.RecommendationHistoryResponse;
import com.insightflow.recommendation.dto.response.RecommendationResponse;
import com.insightflow.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Recommendations", description = "Recommendation insights and analytics")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    @Operation(summary = "List recommendations", description = "Paginated recommendations with filtering")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<ApiResponse<PageResponse<RecommendationResponse>>> getRecommendations(
            @Valid @ModelAttribute RecommendationFilterRequest filter) {
        Page<RecommendationResponse> page = recommendationService.getRecommendations(filter);
        PageResponse<RecommendationResponse> body = PageResponse.from(page);
        return ResponseEntity.ok(ApiResponse.success(body, "Recommendations retrieved successfully"));
    }

    @GetMapping("/{recommendationId}")
    @Operation(summary = "Get recommendation detail")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getRecommendation(
            @PathVariable UUID recommendationId) {
        RecommendationResponse response = recommendationService.getRecommendation(recommendationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Recommendation retrieved successfully"));
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get recommendation analytics")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<ApiResponse<RecommendationAnalyticsResponse>> getAnalytics() {
        RecommendationAnalyticsResponse response = recommendationService.getRecommendationAnalytics();
        return ResponseEntity.ok(ApiResponse.success(response, "Analytics retrieved successfully"));
    }

    @GetMapping("/{recommendationId}/history")
    @Operation(summary = "Get recommendation history")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<ApiResponse<PageResponse<RecommendationHistoryResponse>>> getHistory(
            @PathVariable UUID recommendationId,
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        Page<RecommendationHistoryResponse> page = recommendationService.getRecommendationHistory(recommendationId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page), "History retrieved successfully"));
    }

    @PostMapping("/trigger")
    @Operation(summary = "Manual recommendation trigger")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Recommendation triggered")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<ApiResponse<RecommendationResponse>> triggerManualRecommendation(
            @Valid @RequestBody ManualRecommendationTriggerRequest request) {
        RecommendationResponse response = recommendationService.triggerManualRecommendation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Recommendation triggered successfully"));
    }
}
