package com.insightflow.billing.repository;

import com.insightflow.billing.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Page<PaymentTransaction> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    List<PaymentTransaction> findByStatus(String status);

    // Phát hiện thanh toán trùng: giao dịch SUCCESS gần nhất của cùng tenant + gói sau mốc thời gian.
    Optional<PaymentTransaction> findFirstByTenantIdAndPackageCodeAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID tenantId, String packageCode, String status, LocalDateTime createdAt);

    // Khôi phục tenant_id/package_code khi Redis đã hết hạn: tra giao dịch SUCCESS cùng mã
    // (ca double-pay — webhook trước đã kích hoạt gói và xoá key Redis).
    Optional<PaymentTransaction> findFirstByTransactionCodeAndStatusOrderByCreatedAtDesc(
            String transactionCode, String status);

    // List theo trạng thái + search tuỳ chọn (q null/blank = không lọc theo từ khoá).
    // Tìm trên: mã tham chiếu, nội dung CK, mã đơn IFLOW, mã gói, và tenant_id (dạng text).
    @Query("""
            SELECT t FROM PaymentTransaction t
            WHERE t.status IN :statuses
              AND (:q IS NULL
                   OR LOWER(t.referenceCode)     LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(t.content)           LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(t.transactionCode)   LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(t.packageCode)       LIKE LOWER(CONCAT('%', :q, '%'))
                   OR CAST(t.tenantId AS String) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<PaymentTransaction> searchByStatuses(@Param("statuses") List<String> statuses,
                                              @Param("q") String q,
                                              Pageable pageable);

}