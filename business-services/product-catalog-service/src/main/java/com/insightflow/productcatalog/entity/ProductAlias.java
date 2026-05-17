package com.insightflow.productcatalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_aliases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAlias extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", length = 36, nullable = false)
    private String categoryId;

    @Column(name = "keyword", length = 100, nullable = false, unique = true)
    private String keyword;
}