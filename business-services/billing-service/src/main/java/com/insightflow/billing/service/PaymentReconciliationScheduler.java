package com.insightflow.billing.service;

import com.insightflow.billing.dto.request.UpgradeRequest;
import com.insightflow.billing.entity.BillingPackage;
import com.insightflow.billing.entity.PaymentTransaction;
import com.insightflow.billing.entity.Plan;
import com.insightflow.billing.repository.PackageRepository;
import com.insightflow.billing.repository.PaymentTransactionRepository;
import com.insightflow.billing.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private final PaymentTransactionRepository transactionRepository;
    private final SubscriptionService subscriptionService;
    private final PackageRepository packageRepository;
    private final PlanRepository planRepository;

    // Chạy lúc 2:00 AM mỗi ngày theo giờ VN
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    public void reconcilePayments() {
        log.info("🔍 [RECONCILIATION] Bắt đầu tiến trình đối soát giao dịch SePay lúc 2h sáng...");

        // Tìm các giao dịch kẹt (SYSTEM_ERROR hoặc PENDING) cách đây hơn 1 tiếng
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<PaymentTransaction> stuckTransactions = transactionRepository
                .findByStatusInAndCreatedAtBefore(List.of("SYSTEM_ERROR", "PENDING"), oneHourAgo);

        if (stuckTransactions.isEmpty()) {
            log.info("✅ [RECONCILIATION] Không có giao dịch nào bị kẹt. Hệ thống sạch sẽ!");
            return;
        }

        log.info("⚠️ [RECONCILIATION] Phát hiện {} giao dịch bị kẹt. Tiến hành xử lý...", stuckTransactions.size());

        for (PaymentTransaction tx : stuckTransactions) {
            try {
                // Xử lý từng giao dịch một, tránh chết chùm
                processStuckTransaction(tx);
            } catch (Exception e) {
                log.error("❌ [RECONCILIATION] Lỗi khi xử lý bù giao dịch {}: {}", tx.getSepayId(), e.getMessage());
            }
        }

        log.info("🏁 [RECONCILIATION] Hoàn tất đối soát!");
    }

    @Transactional
    public void processStuckTransaction(PaymentTransaction tx) {
        // 1. Gọi API SePay để check trạng thái thực sự của mã giao dịch này
        boolean isSuccessOnSePay = verifyWithSePayApi(tx.getSepayId());

        if (!isSuccessOnSePay) {
            log.warn("⚠️ [RECONCILIATION] Giao dịch {} không thành công trên hệ thống SePay. Đánh dấu FAILED.", tx.getSepayId());
            tx.setStatus("FAILED_VALIDATION");
            tx.setErrorReason("Đối soát 2h sáng: Thất bại hoặc không tồn tại trên SePay");
            transactionRepository.save(tx);
            return;
        }

        // 2. Nếu SePay báo đã nhận tiền thành công, tiến hành bù gói tự động
        log.info("🔄 [RECONCILIATION] Giao dịch {} thành công trên SePay nhưng kẹt ở DB. Tiến hành bù gói...", tx.getSepayId());

        BillingPackage pkg = packageRepository.findByCodeAndStatus(tx.getPackageCode(), "ACTIVE")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Package: " + tx.getPackageCode()));

        Plan plan = planRepository.findByPackageIdAndBillingCycle(pkg.getId(), "MONTHLY")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Plan tháng"));

        UpgradeRequest upgradeRequest = new UpgradeRequest();
        upgradeRequest.setPlanId(plan.getId());
        upgradeRequest.setBillingCycle("MONTHLY");
        upgradeRequest.setAutoRenew(true);

        // Kích hoạt tái nâng cấp
        subscriptionService.upgradePlan(tx.getTenantId(), upgradeRequest);

        // Lưu sổ cái
        tx.setStatus("SUCCESS");
        transactionRepository.save(tx);
        log.info("🎉 [RECONCILIATION] Đã bù gói thành công cho Tenant {}!", tx.getTenantId());
    }

    // Mock gọi API SePay
    private boolean verifyWithSePayApi(String sepayId) {
        log.info("🌐 Đang gọi API SePay kiểm tra trạng thái thực tế của mã giao dịch: {}", sepayId);
        // TODO: Gắn API HTTP Client (Ví dụ RestTemplate/WebClient) gọi GET /transactions/{sepayId} của SePay vào đây
        return true;
    }
}