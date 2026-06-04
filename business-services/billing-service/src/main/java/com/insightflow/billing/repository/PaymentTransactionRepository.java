package com.insightflow.billing.repository;

import com.insightflow.billing.entity.PaymentTransaction;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findBySepayId(String sepayId);
    List<PaymentTransaction> findByStatusInAndCreatedAtBefore(List<String> statuses, LocalDateTime time);

    Page<PaymentTransaction> findByStatusIn(List<String> statuses, Pageable pageable);

}