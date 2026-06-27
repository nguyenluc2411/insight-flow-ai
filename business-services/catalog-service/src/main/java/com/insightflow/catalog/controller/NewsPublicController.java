package com.insightflow.catalog.controller;

import com.insightflow.catalog.dto.NewsArticleResponse;
import com.insightflow.catalog.dto.NewsRatingRequest;
import com.insightflow.catalog.service.NewsService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.RequiresPermission;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
@Tag(name = "News Public & User", description = "Public news viewing and user rating")
public class NewsPublicController {

    private final NewsService newsService;

    // ----- Public Endpoints (Routed by Gateway without JWT) -----

    @GetMapping("/public/news")
    @Operation(summary = "List published news articles (Public)")
    public Page<NewsArticleResponse> listPublicArticles(
            @PageableDefault(size = 20) Pageable pageable) {
        return newsService.getPublicArticles(pageable);
    }

    @GetMapping("/public/news/{id}")
    @Operation(summary = "Get published news article by ID (Public)")
    public NewsArticleResponse getPublicArticle(
            @PathVariable UUID id) {
        return newsService.getPublicArticle(id);
    }

    // ----- Protected Endpoints (Requires JWT) -----

    @GetMapping("/news/{id}/my-rating")
    @Operation(summary = "Get current user's rating for an article (Requires Login)")
    public java.util.Map<String, Object> getMyRating(
            @CurrentUser UserContext user,
            @PathVariable UUID id) {
        Integer value = newsService.getUserRating(user.userId(), id);
        return java.util.Map.of("ratingValue", value != null ? value : 0);
    }

    @PostMapping("/news/{id}/rate")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Rate a news article (Requires Login)")
    public void rateArticle(
            @CurrentUser UserContext user,
            @PathVariable UUID id,
            @RequestBody NewsRatingRequest request) {
        newsService.rateArticle(user.tenantId(), user.userId(), id, request);
    }
}
