package com.insightflow.sales.repository;

import com.insightflow.sales.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByTenantIdAndPhone(UUID tenantId, String phone);

    Page<Customer> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Customer> findByTenantIdAndId(UUID tenantId, UUID id);
}
