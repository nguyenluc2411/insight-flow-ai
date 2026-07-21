package com.insightflow.billing.repository;

import com.insightflow.billing.entity.TenantContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TenantContactRepository extends JpaRepository<TenantContact, UUID> {
}
