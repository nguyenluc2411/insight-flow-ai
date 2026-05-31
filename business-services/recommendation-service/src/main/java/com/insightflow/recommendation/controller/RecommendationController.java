package com.insightflow.recommendation.controller;

import com.insightflow.recommendation.entity.RecommendationHistory;
import com.insightflow.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/workspace/{id}")
    public ResponseEntity<RecommendationHistory> getAiResult(@PathVariable("id") String workspaceId) {
        RecommendationHistory result = recommendationService.getRecommendationByWorkspace(workspaceId);
        return ResponseEntity.ok(result);
    }
}