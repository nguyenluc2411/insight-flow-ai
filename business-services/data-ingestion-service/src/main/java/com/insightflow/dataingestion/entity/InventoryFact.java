package com.insightflow.dataingestion.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@Entity
@Table(name = "inventory_facts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class InventoryFact extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "variant_id", length = 36, nullable = false)
    private String variantId;

    @Column(name = "workspace_id", length = 36, nullable = false)
    private String workspaceId;

    @Column(name = "warehouse_location", length = 100)
    private String warehouseLocation;

    @Column(name = "cost_price")
    private Double costPrice;

    @Column(name = "retail_price")
    private Double retailPrice;

    @Column(name = "current_price")
    private Double currentPrice;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "quantity_in_stock", nullable = false)
    private Integer quantityInStock;

    @Column(name = "quantity_sold")
    private Integer quantitySold;

    @Column(name = "import_date")
    private LocalDate importDate;

    @Column(name = "last_sold_date")
    private LocalDate lastSoldDate;
}