package com.insightflow.dataingestion.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "products", uniqueConstraints = {
        // Product code is unique per tenant, not globally — two shops may reuse the same code.
        @UniqueConstraint(name = "uq_products_tenant_code", columnNames = {"tenant_id", "product_code"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "product_code", length = 50, nullable = false)
    private String productCode;

    @Column(name = "product_name", length = 255, nullable = false)
    private String productName;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "sub_category", length = 50)
    private String subCategory;

    @Column(name = "target_demographic", length = 50)
    private String targetDemographic;

    @Column(name = "material", length = 100)
    private String material;

    @Column(name = "fit_type", length = 50)
    private String fitType;

    @Column(name = "pattern", length = 50)
    private String pattern;

    @Column(name = "style_context", length = 50)
    private String styleContext;

    @Column(name = "season", length = 50)
    private String season;

    // Cột JSON chứa mọi đặc tính râu ria (cổ áo, tay áo, cạp quần...)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private String attributes;
}