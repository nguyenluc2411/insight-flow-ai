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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing/admin/refunds")
@RequiredArgsConstructor
@Tag(name = "Admin Refunds", description = "Quản lý đối soát và xác nhận hoàn tiền thủ công")
public class AdminRefundController {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final SePayPaymentService sePayPaymentService;

    // billing-service has no Spring method security (permitAll + UserContextFilter),
    // so @PreAuthorize was never enforced. Gate on the gateway-propagated role instead.
    private void requireSuperAdmin(UserContext user) {
        if (user == null || user.roles() == null || !user.roles().contains(SUPER_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requires SUPER_ADMIN role");
        }
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách giao dịch theo trạng thái (có hỗ trợ tìm kiếm)")
    public ResponseEntity<Page<PaymentTransactionResponse>> getTransactions(
            @CurrentUser UserContext adminUser,
            // Mặc định trạng thái chờ hoàn tiền; truyền statuses để xem SUCCESS / REFUNDED / JUNK
            @RequestParam(defaultValue = "PENDING_REFUND") List<String> statuses,
            // Từ khoá tìm kiếm tuỳ chọn: mã tham chiếu, nội dung CK, mã đơn, mã gói, tenant id
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        requireSuperAdmin(adminUser);
        List<String> upperStatuses = statuses.stream().map(String::toUpperCase).toList();
        return ResponseEntity.ok(sePayPaymentService.getTransactionsByStatuses(
                upperStatuses, q,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một giao dịch cần hoàn tiền")
    public ResponseEntity<PaymentTransactionResponse> getTransactionDetail(
            @CurrentUser UserContext adminUser,
            @PathVariable UUID id) {
        requireSuperAdmin(adminUser);
        return ResponseEntity.ok(sePayPaymentService.getTransactionDetail(id));
    }

    @PostMapping("/{id}/confirm-refund")
    @Operation(summary = "Xác nhận đã hoàn tiền thủ công cho khách")
    public ResponseEntity<Map<String, String>> confirmManualRefund(
            @CurrentUser UserContext adminUser,
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> payload) {

        requireSuperAdmin(adminUser);
        String refundNote = (payload != null) ? payload.getOrDefault("note", "") : "";

        log.warn("🚨 Admin [{}] XÁC NHẬN ĐÃ HOÀN TIỀN TAY CHO GIAO DỊCH [{}]", adminUser.userId(), id);

        sePayPaymentService.confirmManualRefund(id, adminUser.userId().toString(), refundNote);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Đã chốt sổ trạng thái hoàn tiền và lưu vết thành công."
        ));
    }

    @PostMapping("/{id}/mark-junk")
    @Operation(summary = "Đánh dấu giao dịch không thuộc hệ thống (chuyển vào mục giao dịch rác)")
    public ResponseEntity<Map<String, String>> markAsJunk(
            @CurrentUser UserContext adminUser,
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> payload) {

        requireSuperAdmin(adminUser);
        String note = (payload != null) ? payload.getOrDefault("note", "") : "";

        log.warn("🗑️ Admin [{}] đánh dấu giao dịch [{}] là KHÔNG thuộc hệ thống", adminUser.userId(), id);
        sePayPaymentService.markAsJunk(id, adminUser.userId().toString(), note);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Đã chuyển giao dịch vào mục giao dịch rác."
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá vĩnh viễn một giao dịch rác")
    public ResponseEntity<Map<String, String>> deleteJunk(
            @CurrentUser UserContext adminUser,
            @PathVariable UUID id) {

        requireSuperAdmin(adminUser);

        log.warn("❌ Admin [{}] XOÁ VĨNH VIỄN giao dịch rác [{}]", adminUser.userId(), id);
        sePayPaymentService.deleteTransaction(id, adminUser.userId().toString());

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Đã xoá vĩnh viễn giao dịch rác."
        ));
    }
}