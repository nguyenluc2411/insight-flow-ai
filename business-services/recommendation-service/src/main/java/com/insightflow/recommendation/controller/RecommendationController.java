package com.insightflow.recommendation.controller;

import com.insightflow.recommendation.entity.RecommendationHistory;
import com.insightflow.recommendation.service.RecommendationService;
import com.insightflow.security.UserContext;
import com.insightflow.security.UserContextHolder;
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
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.tenantId() == null) {
            return ResponseEntity.status(401).build();
        }
        RecommendationHistory result =
                recommendationService.getRecommendationByWorkspace(ctx.tenantId().toString(), workspaceId);
        return ResponseEntity.ok(result);
    }
}