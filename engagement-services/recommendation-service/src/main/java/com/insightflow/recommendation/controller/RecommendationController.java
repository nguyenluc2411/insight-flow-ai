package com.insightflow.recommendation.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recommendations", description = "Recommendation insights and analytics")
public class RecommendationController {
    // Endpoints will be added in later phases.
}
