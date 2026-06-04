package com.insightflow.dataingestion.repository;
import com.insightflow.dataingestion.entity.InventoryFact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface InventoryFactRepository extends JpaRepository<InventoryFact, String> {
    List<InventoryFact> findByTenantIdAndWorkspaceId(String tenantId, String workspaceId);
    Optional<InventoryFact> findByVariantIdAndWorkspaceId(String variantId, String workspaceId);

}