package com.insightflow.productcatalog.repository;


import com.insightflow.productcatalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, String> {
}