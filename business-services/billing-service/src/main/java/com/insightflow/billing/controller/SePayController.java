package com.insightflow.billing.controller;

import com.insightflow.billing.dto.request.SePayWebhookRequest;
import com.insightflow.billing.service.SePayPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "SePay Webhook Integration")
public class SePayController {

    private final SePayPaymentService sePayPaymentService;

    // Lấy mã bí mật từ file cấu hình (application.yml)
    @Value("${sepay.webhook.secret-token}")
    private String webhookSecretToken;

    @PostMapping("/sepay-webhook")
    @Operation(summary = "Lắng nghe Webhook thanh toán từ SePay")
    public ResponseEntity<Map<String, Boolean>> handleWebhook(
            // 1. Chộp lấy cái Token mà thằng gọi API gửi lên
            @RequestHeader(value = "Authorization", defaultValue = "") String authHeader,
            @RequestBody SePayWebhookRequest request) {


        String expectedAuth = "Apikey " + webhookSecretToken;

        if (!expectedAuth.equals(authHeader)) {
            log.error("🚨 [SECURITY] CẢNH BÁO: Phát hiện request giả mạo Webhook SePay hoặc không đúng! Token sai: {}", authHeader);
            // Sút cổ ra ngoài, trả về lỗi 401 Unauthorized ngay lập tức
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false));
        }

        // 3. Đúng Token thì mới cho đi tiếp vào xử lý logic lưu Database
        sePayPaymentService.processWebhook(request);

        return ResponseEntity.ok(Map.of("success", true));
    }
}