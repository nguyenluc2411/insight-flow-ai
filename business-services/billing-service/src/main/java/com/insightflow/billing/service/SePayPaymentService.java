package com.insightflow.billing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.billing.dto.request.SePayWebhookRequest;
import com.insightflow.billing.dto.request.UpgradeRequest;
import com.insightflow.billing.dto.response.PaymentTransactionResponse;
import com.insightflow.billing.dto.response.SubscriptionResponse;
import com.insightflow.billing.entity.BillingPackage;
import com.insightflow.billing.entity.OutboxEvent;
import com.insightflow.billing.entity.PaymentTransaction;
import com.insightflow.billing.entity.TenantContact;
import com.insightflow.billing.entity.TenantSubscription;
import com.insightflow.billing.repository.OutboxRepository;
import com.insightflow.billing.repository.PackageRepository;
import com.insightflow.billing.repository.PaymentTransactionRepository;
import com.insightflow.billing.repository.TenantContactRepository;
import com.insightflow.billing.repository.TenantSubscriptionRepository;
import com.insightflow.common.events.billing.PaymentReceiptEvent;
import com.insightflow.common.web.exception.BusinessException;
import com.insightflow.common.web.exception.ErrorCode;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SePayPaymentService {

    private final SubscriptionService subscriptionService;
    private final PaymentTransactionRepository transactionRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final PackageRepository packageRepository;
    private final TenantContactRepository tenantContactRepository;
    private final OutboxRepository outboxRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Thông tin tài khoản nhận tiền (bên bán) — hiển thị trên biên nhận. Trùng cấu hình CheckoutService.
    @Value("${app.payment.bank-id:MB}")
    private String bankId;
    @Value("${app.payment.account-no:0367457851}")
    private String bankAccountNo;
    @Value("${app.payment.account-name:DOAN TRUNG TRUC}")
    private String bankAccountName;

    private static final DateTimeFormatter INVOICE_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    @PersistenceContext
    private EntityManager entityManager;

    // SePay gửi transactionDate dạng "yyyy-MM-dd HH:mm:ss".
    private static final DateTimeFormatter SEPAY_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Namespace cho Postgres advisory lock — tránh đụng các advisory lock khác (nếu có).
    private static final int ORDER_LOCK_NAMESPACE = 0x53455059; // "SEPY"

    // --- Ngưỡng phát hiện thanh toán trùng (cấu hình qua config, default an toàn) ---
    // (a) Đã có giao dịch SUCCESS cùng gói trong vòng N giờ -> double-pay (chuyển 2 lần sát nhau).
    @Value("${app.payment.duplicate.window-hours:24}")
    private int duplicateWindowHours;
    // (b) Đang ACTIVE đúng plan đó mà còn > N ngày mới hết hạn -> chuyển lại khi gói còn dài hạn
    //     (vẫn cho qua gia hạn sát ngày & đổi gói).
    @Value("${app.payment.duplicate.grace-days:3}")
    private int duplicateGraceDays;

    // =========================================================================
    // 1. LUỒNG XỬ LÝ WEBHOOK TỪ SEPAY
    // =========================================================================
    @Transactional
    public void processWebhook(SePayWebhookRequest request) {
        log.info("💰 [SEPAY] Nhận Webhook ID: {}, Tiền: {}, Nội dung: {}", request.getId(), request.getTransferAmount(), request.getContent());

        if (transactionRepository.findBySepayId(request.getId()).isPresent()) {
            log.info("♻️ [SEPAY] Giao dịch {} đã xử lý. Bỏ qua.", request.getId());
            return;
        }

        String rawContent = request.getContent() != null ? request.getContent().trim().toUpperCase() : "";
        Pattern pattern = Pattern.compile("IFLOW[A-Z0-9]{6}");
        Matcher matcher = pattern.matcher(rawContent);

        String transactionCode = null;
        if (matcher.find()) {
            transactionCode = matcher.group();
        }

        if (transactionCode == null) {
            // Không có mã -> không gắn được vào tenant nào, đành chỉ lưu để đối chứng tay.
            handleBusinessError(request, null, null, null,
                    "Không tìm thấy mã giao dịch (IFLOWxxxxxx) hợp lệ trong nội dung chuyển khoản");
            return;
        }

        // Tuần tự hoá các webhook double-pay cùng mã (chống race-condition đồng thời):
        // webhook thứ hai bị chặn cho tới khi webhook thứ nhất commit xong, nhờ đó nó
        // thấy gói đã kích hoạt (Redis đã bị xoá / detectDuplicateReason bắt được) và
        // đi nhánh chờ-hoàn-tiền thay vì nâng cấp lần nữa.
        lockOrderForUpdate(transactionCode);

        String jsonOrder = redisTemplate.opsForValue().get(transactionCode);

        if (!StringUtils.hasText(jsonOrder)) {
            // Redis hết hạn. Có 2 khả năng:
            //  (1) Double-pay: webhook trước đã kích hoạt gói & xoá key -> khôi phục
            //      tenant_id/package_code từ giao dịch SUCCESS cùng mã để admin đối soát đúng tenant.
            //  (2) Mã thật sự quá hạn/không tồn tại -> không gắn được tenant nào.
            PaymentTransaction prior = transactionRepository
                    .findFirstByTransactionCodeAndStatusOrderByCreatedAtDesc(transactionCode, "SUCCESS")
                    .orElse(null);
            if (prior != null) {
                handleBusinessError(request, transactionCode, prior.getTenantId(), prior.getPackageCode(),
                        String.format("Chuyển trùng: mã %s đã được kích hoạt bởi giao dịch trước (gói %s) — chờ hoàn tiền.",
                                transactionCode, prior.getPackageCode()));
            } else {
                handleBusinessError(request, transactionCode, null, null,
                        "Mã đơn hàng đã hết hạn hoặc không tồn tại (Quá 15p)");
            }
            return;
        }

        try {
            Map<String, Object> orderData = objectMapper.readValue(jsonOrder, new TypeReference<>() {});

            UUID tenantId = UUID.fromString((String) orderData.get("tenantId"));
            UUID planId = UUID.fromString((String) orderData.get("planId"));
            String packageCode = (String) orderData.get("packageCode");
            String billingCycle = (String) orderData.get("billingCycle");

            Integer expectedAmount = Integer.parseInt(String.valueOf(orderData.get("amount")));

            if (request.getTransferAmount() < expectedAmount) {
                handleBusinessError(request, transactionCode, tenantId, packageCode,
                        "Chuyển thiếu tiền. Yêu cầu: " + expectedAmount + ", Thực nhận: " + request.getTransferAmount());
                return;
            }

            // Chống "chuyển 1 gói nhiều lần": nếu phát hiện trùng thì KHÔNG nâng gói lần nữa,
            // gắn cờ chờ hoàn tiền kèm lý do cụ thể để admin đối soát.
            String duplicateReason = detectDuplicateReason(tenantId, planId, packageCode);
            if (duplicateReason != null) {
                handleBusinessError(request, transactionCode, tenantId, packageCode, duplicateReason);
                return;
            }

            UpgradeRequest upgradeRequest = new UpgradeRequest();
            upgradeRequest.setPlanId(planId);
            upgradeRequest.setBillingCycle(billingCycle);
            upgradeRequest.setAutoRenew(true);

            SubscriptionResponse subscription = subscriptionService.upgradePlan(tenantId, upgradeRequest);

            PaymentTransaction tx = PaymentTransaction.builder()
                    .sepayId(request.getId())
                    .tenantId(tenantId)
                    .transactionCode(transactionCode)
                    .packageCode(packageCode)
                    .amount(request.getTransferAmount())
                    .accountNumber(request.getAccountNumber())
                    .senderAccountNumber(request.getSenderAccountNumber())
                    .referenceCode(request.getReferenceCode())
                    .transactionDate(parseTransactionDate(request.getTransactionDate()))
                    .content(request.getContent())
                    .status("SUCCESS")
                    .build();
            transactionRepository.save(tx);

            redisTemplate.delete(transactionCode);
            log.info("✅ [SEPAY] Đã xử lý thành công mã {} cho Tenant {}", transactionCode, tenantId);

            // Phát biên nhận thanh toán qua outbox -> notification-service gửi email cho khách.
            // Bọc try/catch: lỗi tạo biên nhận KHÔNG được làm rollback giao dịch đã thành công.
            try {
                emitPaymentReceipt(tx, subscription, packageCode, billingCycle);
            } catch (Exception ex) {
                log.error("⚠️ [SEPAY] Không tạo được biên nhận cho mã {} (giao dịch vẫn thành công): {}",
                        transactionCode, ex.getMessage(), ex);
            }

        } catch (Exception e) {
            // Rethrow so the tx rolls back and SePay retries — the idempotency guard
            // (findBySepayId) makes the retry safe. Log with stacktrace for diagnosis.
            log.error("❌ Lỗi hệ thống khi xử lý Webhook id={}", request.getId(), e);
            throw new RuntimeException("SePay webhook processing failed for id=" + request.getId(), e);
        }
    }

    /**
     * Dựng dữ liệu biên nhận thanh toán và ghi vào outbox (event {@code payment.success}).
     * OutboxPublisher sẽ đẩy lên Kafka topic {@code billing.payment.success}; notification-service
     * tiêu thụ và gửi email biên nhận chuyên nghiệp cho khách.
     */
    private void emitPaymentReceipt(PaymentTransaction tx, SubscriptionResponse subscription,
                                    String packageCode, String billingCycle) {
        UUID tenantId = tx.getTenantId();

        // Email + tên người nhận: lấy từ cache liên hệ tenant (populate từ auth.tenant.registered).
        TenantContact contact = tenantId != null
                ? tenantContactRepository.findById(tenantId).orElse(null)
                : null;
        String recipientEmail = contact != null ? contact.getEmail() : null;
        String recipientName = contact != null ? contact.getName() : null;

        if (recipientEmail == null || recipientEmail.isBlank()) {
            // Không có email -> vẫn phát event để notification log lại, nhưng cảnh báo rõ ràng.
            log.warn("📧 [SEPAY] Chưa có email liên hệ cho tenant={} — biên nhận sẽ không gửi được. "
                    + "Hãy chắc chắn tenant đăng ký sau khi bật cache contact.", tenantId);
        }

        // Tên gói hiển thị (fallback về mã gói nếu không tra được).
        String packageName = packageRepository.findByCodeAndStatus(packageCode, "ACTIVE")
                .map(BillingPackage::getName)
                .orElse(packageCode);

        LocalDateTime now = LocalDateTime.now();
        String invoiceNumber = "HD" + INVOICE_NO_DATE.format(now.toLocalDate()) + "-" + tx.getTransactionCode();

        PaymentReceiptEvent receipt = PaymentReceiptEvent.builder()
                .eventId(tx.getId() != null ? tx.getId().toString() : tx.getSepayId())
                .invoiceNumber(invoiceNumber)
                .issuedAt(now.toString())
                .tenantId(tenantId != null ? tenantId.toString() : null)
                .recipientEmail(recipientEmail)
                .recipientName(recipientName)
                .packageCode(packageCode)
                .packageName(packageName)
                .billingCycle(billingCycle)
                .amount(tx.getAmount() != null ? tx.getAmount().longValue() : null)
                .currency("VND")
                .transactionCode(tx.getTransactionCode())
                .sepayId(tx.getSepayId())
                .transactionDate(tx.getTransactionDate() != null ? tx.getTransactionDate().toString() : null)
                .paymentMethod("Chuyển khoản ngân hàng (SePay)")
                .bankName(bankId)
                .bankAccountNo(bankAccountNo)
                .bankAccountName(bankAccountName)
                .startDate(subscription != null && subscription.getStartDate() != null
                        ? subscription.getStartDate().toString() : null)
                .endDate(subscription != null && subscription.getEndDate() != null
                        ? subscription.getEndDate().toString() : null)
                .build();

        Map<String, Object> payload = objectMapper.convertValue(receipt, new TypeReference<>() {});

        outboxRepository.save(OutboxEvent.builder()
                .aggregateId(tenantId != null ? tenantId : tx.getId())
                .eventType("payment.success")
                .payload(payload)
                .build());

        log.info("🧾 [SEPAY] Đã tạo biên nhận {} cho tenant={} email={}", invoiceNumber, tenantId, recipientEmail);
    }

    private void handleBusinessError(SePayWebhookRequest request, String transactionCode,
                                     UUID tenantId, String packageCode, String reason) {
        log.warn("⚠️ [SEPAY] Giao dịch lỗi: {}. Lưu trạng thái chờ hoàn tiền.", reason);
        PaymentTransaction tx = PaymentTransaction.builder()
                .sepayId(request.getId())
                .tenantId(tenantId)              // có thể null nếu không match được mã; admin tra email qua auth-service theo tenantId
                .transactionCode(transactionCode)
                .packageCode(packageCode)
                .amount(request.getTransferAmount())
                .accountNumber(request.getAccountNumber())
                .senderAccountNumber(request.getSenderAccountNumber())
                .referenceCode(request.getReferenceCode())
                .transactionDate(parseTransactionDate(request.getTransactionDate()))
                .content(request.getContent())
                .status("PENDING_REFUND") // CHỐT LUÔN TRẠNG THÁI NÀY
                .errorReason(reason)
                .build();
        transactionRepository.save(tx);
    }

    /**
     * Phát hiện "chuyển 1 gói nhiều lần". Trả về lý do (lưu lại cho admin đối soát) nếu là
     * thanh toán trùng, ngược lại null. Dùng 2 tín hiệu bổ trợ nhau:
     *
     * (a) Đã có giao dịch SUCCESS cùng (tenant, packageCode) trong {@code duplicateWindowHours} giờ
     *     → bắt chính xác ca chuyển 2 lần sát nhau (double-pay), không nhầm với gia hạn tháng sau.
     * (b) Tenant đang ACTIVE đúng plan đó và còn > {@code duplicateGraceDays} ngày mới hết hạn
     *     → bắt ca chuyển lại khi gói vẫn còn dài hạn; vẫn cho qua gia hạn sát ngày, đổi gói,
     *       và lần đầu kích hoạt từ TRIAL.
     */
    private String detectDuplicateReason(UUID tenantId, UUID planId, String packageCode) {
        // (a) Giao dịch SUCCESS gần đây cho cùng gói.
        if (StringUtils.hasText(packageCode)) {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(duplicateWindowHours);
            PaymentTransaction recent = transactionRepository
                    .findFirstByTenantIdAndPackageCodeAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                            tenantId, packageCode, "SUCCESS", cutoff)
                    .orElse(null);
            if (recent != null) {
                return String.format(
                        "Chuyển trùng: đã có giao dịch thành công cho gói %s lúc %s (mã %s) trong vòng %d giờ qua — nghi thanh toán nhiều lần.",
                        packageCode, recent.getCreatedAt(), recent.getTransactionCode(), duplicateWindowHours);
            }
        }

        // (b) Đang ACTIVE đúng plan này và còn xa ngày hết hạn.
        TenantSubscription current = subscriptionRepository.findActiveOrTrialByTenantId(tenantId).orElse(null);
        if (current != null
                && "ACTIVE".equalsIgnoreCase(current.getStatus())
                && planId.equals(current.getPlanId())
                && current.getEndDate() != null
                && current.getEndDate().isAfter(LocalDate.now().plusDays(duplicateGraceDays))) {
            return String.format(
                    "Chuyển trùng: tenant đang dùng đúng gói này (hết hạn %s, còn xa ngày gia hạn) — nghi thanh toán lại cho gói đang hoạt động.",
                    current.getEndDate());
        }

        return null;
    }

    /**
     * Khoá xử lý theo từng mã đơn bằng Postgres advisory lock cấp transaction
     * ({@code pg_advisory_xact_lock}). Lock tự giải phóng khi transaction commit/rollback.
     *
     * <p>Hai webhook double-pay cùng {@code transactionCode} sẽ bị tuần tự hoá: cái thứ
     * hai chỉ chạy tiếp sau khi cái thứ nhất hoàn tất, nên không còn nâng cấp trùng —
     * nó rơi vào nhánh Redis-hết-hạn (khôi phục tenant) hoặc bị detectDuplicateReason
     * bắt, rồi lưu PENDING_REFUND để admin hoàn tiền.</p>
     */
    @SuppressWarnings("unchecked")
    private void lockOrderForUpdate(String transactionCode) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:ns, hashtext(:code))")
                .setParameter("ns", ORDER_LOCK_NAMESPACE)
                .setParameter("code", transactionCode)
                .getResultList();
    }

    private LocalDateTime parseTransactionDate(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            return LocalDateTime.parse(raw.trim(), SEPAY_DATE);
        } catch (Exception e) {
            log.warn("⚠️ [SEPAY] Không parse được transactionDate '{}': {}", raw, e.getMessage());
            return null;
        }
    }

    // =========================================================================
    // 2. LUỒNG XÁC NHẬN HOÀN TIỀN THỦ CÔNG (DÀNH CHO ADMIN)
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<PaymentTransactionResponse> getTransactionsByStatuses(List<String> statuses, String q, Pageable pageable) {
        String keyword = StringUtils.hasText(q) ? q.trim() : null;
        if (keyword == null) {
            return transactionRepository.findByStatusIn(statuses, pageable).map(this::toResponse);
        }
        return transactionRepository.searchByStatuses(statuses, keyword, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PaymentTransactionResponse getTransactionDetail(UUID transactionId) {
        PaymentTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch: " + transactionId));
        return toResponse(tx);
    }

    @Transactional
    public void confirmManualRefund(UUID transactionId, String adminId, String refundNote) {
        // 1. Chỉ đơn giản là móc giao dịch lên
        PaymentTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch: " + transactionId));

        // 2. Chốt chặn Bảo mật & Logic (Chỉ duy nhất PENDING_REFUND mới được đi tiếp)
        if (!"PENDING_REFUND".equals(tx.getStatus())) {
            log.error("💥 [SECURITY] Admin {} cố tình hoàn tiền cho giao dịch {} đang ở trạng thái {}", adminId, transactionId, tx.getStatus());
            throw new BusinessException(ErrorCode.CONFLICT, "Giao dịch đã được xử lý hoặc không nằm trong danh sách cần hoàn tiền!");
        }

        // 3. Đánh dấu hoàn tất & Lưu Audit Trail (Chuyển 1 phát sang REFUNDED)
        tx.setStatus("REFUNDED");
        String auditTrail = String.format(" | [Đã đối soát và hoàn tiền tay bởi Admin: %s. Ghi chú: %s]",
                adminId, StringUtils.hasText(refundNote) ? refundNote : "N/A");
        tx.setErrorReason(tx.getErrorReason() + auditTrail);

        transactionRepository.save(tx);
        log.info("✅ [ADMIN] Đã xác nhận hoàn tiền thủ công cho giao dịch {}", transactionId);

        // 4. Bắn sự kiện ra Kafka
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("transactionId", tx.getId().toString());
        eventPayload.put("sepayId", tx.getSepayId());
        eventPayload.put("amount", tx.getAmount());
        eventPayload.put("senderAccountNumber", tx.getSenderAccountNumber());

        UUID aggregateId = tx.getTenantId() != null ? tx.getTenantId() : tx.getId();

        outboxRepository.save(OutboxEvent.builder()
                .aggregateId(aggregateId)
                .eventType("refund.success")
                .payload(eventPayload)
                .build());
    }

    /**
     * Đánh dấu một giao dịch đang chờ hoàn tiền là "không thuộc hệ thống" và xếp vào
     * mục giao dịch rác (vd: chuyển khoản lạ, người ngoài lỡ chuyển vào). Chỉ
     * PENDING_REFUND mới được chuyển. Lưu vết admin để truy nguyên.
     */
    @Transactional
    public void markAsJunk(UUID transactionId, String adminId, String note) {
        PaymentTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch: " + transactionId));

        if (!"PENDING_REFUND".equals(tx.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Chỉ giao dịch đang chờ hoàn tiền mới được đánh dấu là giao dịch rác!");
        }

        tx.setStatus("JUNK");
        String auditTrail = String.format(" | [Đánh dấu KHÔNG thuộc hệ thống bởi Admin: %s. Ghi chú: %s]",
                adminId, StringUtils.hasText(note) ? note : "N/A");
        tx.setErrorReason((tx.getErrorReason() != null ? tx.getErrorReason() : "") + auditTrail);

        transactionRepository.save(tx);
        log.info("🗑️ [ADMIN] Admin {} đã chuyển giao dịch {} vào mục giao dịch rác", adminId, transactionId);
    }

    /**
     * Xoá vĩnh viễn một giao dịch rác khỏi DB. Chốt chặn: chỉ xoá được giao dịch
     * đã ở trạng thái JUNK (không thể xoá nhầm giao dịch SUCCESS / đang chờ hoàn tiền).
     */
    @Transactional
    public void deleteTransaction(UUID transactionId, String adminId) {
        PaymentTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch: " + transactionId));

        if (!"JUNK".equals(tx.getStatus())) {
            log.error("💥 [SECURITY] Admin {} cố xoá giao dịch {} không phải rác (status={})",
                    adminId, transactionId, tx.getStatus());
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Chỉ giao dịch trong mục rác mới được xoá vĩnh viễn!");
        }

        transactionRepository.delete(tx);
        log.warn("❌ [ADMIN] Admin {} đã XOÁ VĨNH VIỄN giao dịch rác {}", adminId, transactionId);
    }

    private PaymentTransactionResponse toResponse(PaymentTransaction tx) {
        return PaymentTransactionResponse.builder()
                .id(tx.getId())
                .sepayId(tx.getSepayId())
                .tenantId(tx.getTenantId())
                .packageCode(tx.getPackageCode())
                .amount(tx.getAmount())
                .accountNumber(tx.getAccountNumber())
                .senderAccountNumber(tx.getSenderAccountNumber())
                .referenceCode(tx.getReferenceCode())
                .content(tx.getContent())
                .status(tx.getStatus())
                .errorReason(tx.getErrorReason())
                .transactionDate(tx.getTransactionDate())
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .build();
    }
}