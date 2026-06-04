package com.insightflow.billing.controller;

import com.insightflow.billing.dto.request.CheckoutRequest;
import com.insightflow.billing.dto.response.CheckoutResponse;
import com.insightflow.billing.service.CheckoutService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing/checkout")
@RequiredArgsConstructor
@Tag(name = "Checkout", description = "Khởi tạo thanh toán & QR Code")
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping
    @Operation(summary = "Tạo mã QR thanh toán động cho từng User (Không lưu DB)")
    @ApiResponse(responseCode = "200", description = "Thành công lấy mã QR")
    public ResponseEntity<CheckoutResponse> createCheckout(
            @CurrentUser UserContext user,
            @Valid @RequestBody CheckoutRequest request) {

        return ResponseEntity.ok(checkoutService.generateCheckoutInfo(user.tenantId(), request));
    }
}