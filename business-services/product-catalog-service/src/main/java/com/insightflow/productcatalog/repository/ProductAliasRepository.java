package com.insightflow.productcatalog.repository;

import com.insightflow.productcatalog.entity.ProductAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductAliasRepository extends JpaRepository<ProductAlias, Long> {
    Optional<ProductAlias> findByKeywordIgnoreCase(String keyword);
}