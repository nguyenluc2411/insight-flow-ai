package com.insightflow.productcatalog.repository;

import com.insightflow.productcatalog.entity.CatalogDictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogDictionaryRepository extends JpaRepository<CatalogDictionary, String> {
}