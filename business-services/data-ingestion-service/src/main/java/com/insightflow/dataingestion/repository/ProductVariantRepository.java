package com.insightflow.dataingestion.repository;
import com.insightflow.dataingestion.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
    Optional<ProductVariant> findBySku(String sku);
}