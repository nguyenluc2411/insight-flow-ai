package com.insightflow.billing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.billing.dto.request.SePayWebhookRequest;
import com.insightflow.billing.dto.request.UpgradeRequest;
import com.insightflow.billing.dto.response.PaymentTransactionResponse;
import com.insightflow.billing.entity.OutboxEvent;
import com.insightflow.billing.entity.PaymentTransaction;
import com.insightflow.billing.repository.OutboxRepository;
import com.insightflow.billing.repository.PaymentTransactionRepository;
import com.insightflow.common.web.exception.BusinessException;
import com.insightflow.common.web.exception.ErrorCode;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final OutboxRepository outboxRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

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
            handleBusinessError(request, null, "Không tìm thấy mã giao dịch (IFLOWxxxxxx) hợp lệ trong nội dung chuyển khoản");
            return;
        }

        String jsonOrder = redisTemplate.opsForValue().get(transactionCode);

        if (!StringUtils.hasText(jsonOrder)) {
            handleBusinessError(request, transactionCode, "Mã đơn hàng đã hết hạn hoặc không tồn tại (Quá 15p)");
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
                handleBusinessError(request, transactionCode, "Chuyển thiếu tiền. Yêu cầu: " + expectedAmount + ", Thực nhận: " + request.getTransferAmount());
                return;
            }

            UpgradeRequest upgradeRequest = new UpgradeRequest();
            upgradeRequest.setPlanId(planId);
            upgradeRequest.setBillingCycle(billingCycle);
            upgradeRequest.setAutoRenew(true);

            subscriptionService.upgradePlan(tenantId, upgradeRequest);

            PaymentTransaction tx = PaymentTransaction.builder()
                    .sepayId(request.getId())
                    .tenantId(tenantId)
                    .transactionCode(transactionCode)
                    .packageCode(packageCode)
                    .amount(request.getTransferAmount())
                    .accountNumber(request.getAccountNumber())
                    .senderAccountNumber(request.getSenderAccountNumber())
                    .content(request.getContent())
                    .status("SUCCESS")
                    .build();
            transactionRepository.save(tx);

            redisTemplate.delete(transactionCode);
            log.info("✅ [SEPAY] Đã xử lý thành công mã {} cho Tenant {}", transactionCode, tenantId);

        } catch (Exception e) {
            // Rethrow so the tx rolls back and SePay retries — the idempotency guard
            // (findBySepayId) makes the retry safe. Log with stacktrace for diagnosis.
            log.error("❌ Lỗi hệ thống khi xử lý Webhook id={}", request.getId(), e);
            throw new RuntimeException("SePay webhook processing failed for id=" + request.getId(), e);
        }
    }

    private void handleBusinessError(SePayWebhookRequest request, String transactionCode, String reason) {
        log.warn("⚠️ [SEPAY] Giao dịch lỗi: {}. Lưu trạng thái chờ hoàn tiền.", reason);
        PaymentTransaction tx = PaymentTransaction.builder()
                .sepayId(request.getId())
                .transactionCode(transactionCode)
                .amount(request.getTransferAmount())
                .accountNumber(request.getAccountNumber())
                .senderAccountNumber(request.getSenderAccountNumber())
                .content(request.getContent())
                .status("PENDING_REFUND") // CHỐT LUÔN TRẠNG THÁI NÀY
                .errorReason(reason)
                .build();
        transactionRepository.save(tx);
    }

    // =========================================================================
    // 2. LUỒNG XÁC NHẬN HOÀN TIỀN THỦ CÔNG (DÀNH CHO ADMIN)
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<PaymentTransactionResponse> getTransactionsByStatuses(List<String> statuses, Pageable pageable) {
        return transactionRepository.findByStatusIn(statuses, pageable).map(this::toResponse);
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

    private PaymentTransactionResponse toResponse(PaymentTransaction tx) {
        return PaymentTransactionResponse.builder()
                .id(tx.getId())
                .sepayId(tx.getSepayId())
                .tenantId(tx.getTenantId())
                .packageCode(tx.getPackageCode())
                .amount(tx.getAmount())
                .accountNumber(tx.getAccountNumber())
                .senderAccountNumber(tx.getSenderAccountNumber())
                .content(tx.getContent())
                .status(tx.getStatus())
                .errorReason(tx.getErrorReason())
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .build();
    }
}