package com.insightflow.billing.controller;

import com.insightflow.billing.dto.response.PaymentTransactionResponse;
import com.insightflow.billing.service.SePayPaymentService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing/admin/refunds")
@RequiredArgsConstructor
@Tag(name = "Admin Refunds", description = "Quản lý đối soát và xác nhận hoàn tiền thủ công")
public class AdminRefundController {

    private final SePayPaymentService sePayPaymentService;

    @GetMapping
    @Operation(summary = "Lấy danh sách giao dịch lỗi cần đối soát")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<Page<PaymentTransactionResponse>> getTransactions(
            @CurrentUser UserContext adminUser,
            // SỬA Ở ĐÂY: Chỉ mặc định tìm đúng 1 trạng thái chờ hoàn tiền
            @RequestParam(defaultValue = "PENDING_REFUND") List<String> statuses,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<String> upperStatuses = statuses.stream().map(String::toUpperCase).toList();
        return ResponseEntity.ok(sePayPaymentService.getTransactionsByStatuses(
                upperStatuses,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một giao dịch cần hoàn tiền")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<PaymentTransactionResponse> getTransactionDetail(
            @CurrentUser UserContext adminUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(sePayPaymentService.getTransactionDetail(id));
    }

    @PostMapping("/{id}/confirm-refund")
    @Operation(summary = "Xác nhận đã hoàn tiền thủ công cho khách")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> confirmManualRefund(
            @CurrentUser UserContext adminUser,
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> payload) {

        String refundNote = (payload != null) ? payload.getOrDefault("note", "") : "";

        log.warn("🚨 Admin [{}] XÁC NHẬN ĐÃ HOÀN TIỀN TAY CHO GIAO DỊCH [{}]", adminUser.userId(), id);

        sePayPaymentService.confirmManualRefund(id, adminUser.userId().toString(), refundNote);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Đã chốt sổ trạng thái hoàn tiền và lưu vết thành công."
        ));
    }
}