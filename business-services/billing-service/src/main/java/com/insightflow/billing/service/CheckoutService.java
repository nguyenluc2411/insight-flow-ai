package com.insightflow.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.billing.dto.request.CheckoutRequest;
import com.insightflow.billing.dto.response.CheckoutResponse;
import com.insightflow.billing.entity.BillingPackage;
import com.insightflow.billing.entity.Plan;
import com.insightflow.billing.repository.PackageRepository;
import com.insightflow.billing.repository.PlanRepository;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckoutService {

    private final PackageRepository packageRepository;
    private final PlanRepository planRepository;

    // Thêm công cụ chơi với Redis
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.payment.bank-id:MB}")
    private String bankId;

    @Value("${app.payment.account-no:0367457851}")
    private String accountNo;

    @Value("${app.payment.account-name:DOAN TRUNG TRUC}")
    private String accountName;

    public CheckoutResponse generateCheckoutInfo(UUID tenantId, CheckoutRequest request) {

        String cleanCycle = request.getBillingCycle().trim().toUpperCase();
        String cleanPackageCode = request.getPackageCode().trim().toUpperCase();

        // 1. Lấy thông tin Gói và Plan từ DB
        BillingPackage pkg = packageRepository.findByCodeAndStatus(cleanPackageCode, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Gói cước không tồn tại hoặc đã ngưng hoạt động: " + cleanPackageCode));

        Plan plan = planRepository.findByPackageIdAndBillingCycle(pkg.getId(), cleanCycle)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chu kỳ " + cleanCycle + " cho gói " + cleanPackageCode));

        // 2. Sinh mã giao dịch SIÊU NGẮN (Ví dụ: IFLOW82A4F1)
        String shortCode = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        String transactionCode = "IFLOW" + shortCode;

        // 3. Đóng gói Data và ném vào Redis (Sống 15 phút)
        try {
            Map<String, Object> orderData = Map.of(
                    "tenantId", tenantId.toString(),
                    "planId", plan.getId().toString(), // Lưu luôn PlanID để Webhook khỏi phải query DB lại
                    "packageCode", pkg.getCode(),
                    "billingCycle", plan.getBillingCycle(),
                    "amount", plan.getPriceVnd()
            );
            String jsonOrder = objectMapper.writeValueAsString(orderData);

            // Set TTL 15 phút
            redisTemplate.opsForValue().set(transactionCode, jsonOrder, 15, TimeUnit.MINUTES);
            log.info("🛒 [CHECKOUT] Đã tạo QR & lưu Redis nháp 15 phút cho mã: {}", transactionCode);

        } catch (Exception e) {
            log.error("❌ Lỗi hệ thống khi lưu Redis: {}", e.getMessage());
            throw new RuntimeException("Lỗi nội bộ khi tạo mã thanh toán");
        }

        // 4. Sinh URL QR với nội dung siêu ngắn chống cắt chữ
        String encodedContent = URLEncoder.encode(transactionCode, StandardCharsets.UTF_8);
        String encodedName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);

        String qrUrl = String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                bankId, accountNo, plan.getPriceVnd(), encodedContent, encodedName);

        return CheckoutResponse.builder()
                .qrUrl(qrUrl)
                .amount(plan.getPriceVnd())
                .content(transactionCode) // Trả về mã ngắn
                .bankId(bankId)
                .accountNo(accountNo)
                .accountName(accountName)
                .build();
    }
}