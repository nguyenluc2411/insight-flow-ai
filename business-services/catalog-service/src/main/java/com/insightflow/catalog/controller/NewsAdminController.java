package com.insightflow.catalog.controller;

import com.insightflow.catalog.dto.NewsArticleRequest;
import com.insightflow.catalog.dto.NewsArticleResponse;
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
@RequestMapping("/api/v1/catalog/admin/news")
@RequiredArgsConstructor
@Tag(name = "News Admin", description = "Admin news management")
public class NewsAdminController {

    private final NewsService newsService;

    @GetMapping
    @RequiresPermission("catalog:write") // Or a specific news:read permission if defined
    @Operation(summary = "List all news articles for admin")
    public Page<NewsArticleResponse> listAdminArticles(
            @CurrentUser UserContext user,
            @PageableDefault(size = 20) Pageable pageable) {
        return newsService.getAdminArticles(user.tenantId(), pageable);
    }

    @PostMapping
    @RequiresPermission("catalog:write")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create news article")
    public NewsArticleResponse createArticle(
            @CurrentUser UserContext user,
            @RequestBody NewsArticleRequest request) {
        return newsService.createArticle(user.tenantId(), user.userId(), request);
    }

    @PutMapping("/{id}")
    @RequiresPermission("catalog:write")
    @Operation(summary = "Update news article")
    public NewsArticleResponse updateArticle(
            @CurrentUser UserContext user,
            @PathVariable UUID id,
            @RequestBody NewsArticleRequest request) {
        return newsService.updateArticle(user.tenantId(), id, request);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("catalog:write")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete news article")
    public void deleteArticle(
            @CurrentUser UserContext user,
            @PathVariable UUID id) {
        newsService.deleteArticle(user.tenantId(), id);
    }
}
