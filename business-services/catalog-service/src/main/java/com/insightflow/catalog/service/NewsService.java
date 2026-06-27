package com.insightflow.catalog.service;

import com.insightflow.catalog.dto.NewsArticleRequest;
import com.insightflow.catalog.dto.NewsArticleResponse;
import com.insightflow.catalog.dto.NewsRatingRequest;
import com.insightflow.catalog.entity.NewsArticle;
import com.insightflow.catalog.entity.NewsRating;
import com.insightflow.catalog.repository.NewsArticleRepository;
import com.insightflow.catalog.repository.NewsRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsArticleRepository newsArticleRepository;
    private final NewsRatingRepository newsRatingRepository;

    @Transactional
    public NewsArticleResponse createArticle(UUID tenantId, UUID authorId, NewsArticleRequest request) {
        NewsArticle article = new NewsArticle();
        article.setTenantId(tenantId);
        article.setAuthorId(authorId);
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCoverImageUrl(request.getCoverImageUrl());
        article.setStatus(request.getStatus() != null ? request.getStatus() : "DRAFT");
        if ("PUBLISHED".equals(article.getStatus())) {
            article.setPublishedAt(Instant.now());
        }
        
        NewsArticle saved = newsArticleRepository.save(article);
        return mapToResponse(saved);
    }

    @Transactional
    public NewsArticleResponse updateArticle(UUID tenantId, UUID articleId, NewsArticleRequest request) {
        NewsArticle article = newsArticleRepository.findByIdAndTenantId(articleId, tenantId)
                .orElseThrow(() -> new RuntimeException("News article not found"));
                
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCoverImageUrl(request.getCoverImageUrl());
        
        if (!"PUBLISHED".equals(article.getStatus()) && "PUBLISHED".equals(request.getStatus())) {
            article.setPublishedAt(Instant.now());
        }
        article.setStatus(request.getStatus());
        
        NewsArticle saved = newsArticleRepository.save(article);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteArticle(UUID tenantId, UUID articleId) {
        NewsArticle article = newsArticleRepository.findByIdAndTenantId(articleId, tenantId)
                .orElseThrow(() -> new RuntimeException("News article not found"));
        newsArticleRepository.delete(article);
    }

    @Transactional(readOnly = true)
    public Page<NewsArticleResponse> getAdminArticles(UUID tenantId, Pageable pageable) {
        return newsArticleRepository.findByTenantId(tenantId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<NewsArticleResponse> getPublicArticles(Pageable pageable) {
        return newsArticleRepository.findByStatus("PUBLISHED", pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public NewsArticleResponse getPublicArticle(UUID articleId) {
        NewsArticle article = newsArticleRepository.findById(articleId)
                .filter(a -> "PUBLISHED".equals(a.getStatus()))
                .orElseThrow(() -> new RuntimeException("News article not found"));
        return mapToResponse(article);
    }

    @Transactional(readOnly = true)
    public Integer getUserRating(UUID userId, UUID articleId) {
        return newsRatingRepository.findByNewsArticleIdAndUserId(articleId, userId)
                .map(NewsRating::getRatingValue)
                .orElse(null);
    }

    @Transactional
    public void rateArticle(UUID tenantId, UUID userId, UUID articleId, NewsRatingRequest request) {
        // findById without tenant filter — public articles can be rated by any authenticated user
        NewsArticle article = newsArticleRepository.findById(articleId)
                .filter(a -> "PUBLISHED".equals(a.getStatus()))
                .orElseThrow(() -> new RuntimeException("News article not found or not published"));

        Optional<NewsRating> existingOpt = newsRatingRepository.findByNewsArticleIdAndUserId(articleId, userId);
        if (existingOpt.isPresent()) {
            NewsRating existing = existingOpt.get();
            existing.setRatingValue(request.getRatingValue());
            newsRatingRepository.save(existing);
        } else {
            NewsRating rating = new NewsRating();
            rating.setNewsArticle(article);
            rating.setUserId(userId);
            rating.setTenantId(tenantId);
            rating.setRatingValue(request.getRatingValue());
            newsRatingRepository.save(rating);
        }
    }

    private NewsArticleResponse mapToResponse(NewsArticle article) {
        NewsArticleResponse response = new NewsArticleResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setSummary(article.getSummary());
        response.setContent(article.getContent());
        response.setCoverImageUrl(article.getCoverImageUrl());
        response.setAuthorId(article.getAuthorId());
        response.setStatus(article.getStatus());
        response.setPublishedAt(article.getPublishedAt());
        response.setCreatedAt(article.getCreatedAt());
        response.setUpdatedAt(article.getUpdatedAt());
        
        Double avgRating = newsRatingRepository.getAverageRatingForArticle(article.getId());
        Long ratingCount = newsRatingRepository.getRatingCountForArticle(article.getId());
        
        response.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        response.setRatingCount(ratingCount != null ? ratingCount : 0L);
        return response;
    }
}
