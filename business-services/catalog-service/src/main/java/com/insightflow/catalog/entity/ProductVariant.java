package com.insightflow.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "product_variants", schema = "catalog_db")
@Getter
@Setter
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 150)
    private String sku;

    @Column(length = 100)
    private String barcode;

    @Column(length = 20)
    private String size;

    @Column(length = 50)
    private String color;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "selling_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "compare_at_price", precision = 15, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(nullable = false, length = 20)
    private String status = "active";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "external_ids", columnDefinition = "jsonb")
    private Map<String, Object> externalIds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
