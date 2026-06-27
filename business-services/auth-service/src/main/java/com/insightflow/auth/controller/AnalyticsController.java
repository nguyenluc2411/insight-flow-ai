package com.insightflow.auth.controller;

import com.insightflow.auth.dto.AnalyticsEventRequest;
import com.insightflow.auth.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Public analytics tracking")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/track")
    @Operation(summary = "Track a public visitor event")
    public ResponseEntity<Void> trackEvent(@Valid @RequestBody AnalyticsEventRequest request) {
        analyticsService.trackEvent(request);
        return ResponseEntity.accepted().build();
    }
}
