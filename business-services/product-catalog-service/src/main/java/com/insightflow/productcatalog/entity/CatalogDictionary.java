package com.insightflow.productcatalog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "catalog_dictionaries", indexes = {
        @Index(name = "idx_attribute_synonym", columnList = "attribute_type, synonym")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogDictionary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Loại thuộc tính: COLOR, MATERIAL, DEPARTMENT, CATEGORY, SUB_CATEGORY, FIT_TYPE
    @Column(name = "attribute_type", length = 50, nullable = false)
    private String attributeType;

    // Giá trị chuẩn hóa gửi cho AI (Ví dụ: Red, Cotton, Top, T-Shirt, Oversize)
    @Column(name = "standard_value", length = 100, nullable = false)
    private String standardValue;

    // Từ khóa lóng, từ đồng nghĩa hoặc sai chính tả (Ví dụ: đỏ đô, mận, coton, ôm body)
    @Column(name = "synonym", length = 100, nullable = false)
    private String synonym;
}