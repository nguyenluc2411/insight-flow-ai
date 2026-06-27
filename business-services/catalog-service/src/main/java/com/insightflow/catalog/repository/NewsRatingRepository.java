package com.insightflow.catalog.repository;

import com.insightflow.catalog.entity.NewsRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NewsRatingRepository extends JpaRepository<NewsRating, UUID> {
    Optional<NewsRating> findByNewsArticleIdAndUserId(UUID newsArticleId, UUID userId);

    @Query("SELECT AVG(r.ratingValue) FROM NewsRating r WHERE r.newsArticle.id = :articleId")
    Double getAverageRatingForArticle(@Param("articleId") UUID articleId);
    
    @Query("SELECT COUNT(r) FROM NewsRating r WHERE r.newsArticle.id = :articleId")
    Long getRatingCountForArticle(@Param("articleId") UUID articleId);
}
