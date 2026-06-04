package com.insightflow.dataingestion.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "product_variants", uniqueConstraints = {
        // SKU is unique per tenant, not globally — two shops may reuse the same SKU.
        @UniqueConstraint(name = "uq_variants_tenant_sku", columnNames = {"tenant_id", "sku"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "product_id", length = 36, nullable = false)
    private String productId;

    @Column(name = "sku", length = 100, nullable = false)
    private String sku;

    @Column(name = "color_family", length = 50)
    private String colorFamily;

    @Column(name = "color_name", length = 50)
    private String colorName;

    @Column(name = "size", length = 20)
    private String size;
}