package com.insightflow.dataingestion.repository;

import com.insightflow.dataingestion.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    // Tìm sản phẩm gốc theo Mã sản phẩm — luôn scope theo tenant (mã có thể trùng giữa các shop).
    Optional<Product> findByTenantIdAndProductCode(String tenantId, String productCode);
}