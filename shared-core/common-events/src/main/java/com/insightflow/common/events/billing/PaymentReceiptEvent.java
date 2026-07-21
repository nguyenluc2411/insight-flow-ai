package com.insightflow.common.events.billing;

import lombok.Builder;

/**
 * Sự kiện "thanh toán thành công" do billing-service phát ra (topic {@code billing.payment.success}).
 *
 * <p>notification-service tiêu thụ event này để gửi email <b>biên nhận thanh toán</b> cho khách.
 * Mọi trường ngày/giờ đều là chuỗi ISO để an toàn khi đi qua JSON/outbox JSONB.</p>
 *
 * <p>Tên field dùng camelCase (khớp mặc định Jackson ở cả billing lẫn notification — KHÔNG đặt
 * {@code @JsonNaming(SnakeCase)} để tránh lệch key khi deserialize).</p>
 */
@Builder
public record PaymentReceiptEvent(
        // định danh & idempotency
        String eventId,          // id outbox event — dùng để chống gửi trùng
        String invoiceNumber,    // số biên nhận, vd HD20260721-IFLOW82A4F1
        String issuedAt,         // thời điểm phát hành (ISO-8601)

        // người nhận
        String tenantId,
        String recipientEmail,
        String recipientName,

        // gói dịch vụ
        String packageCode,
        String packageName,
        String billingCycle,     // MONTHLY / YEARLY / TRIAL

        // số tiền
        Long amount,
        String currency,         // VND

        // thông tin giao dịch
        String transactionCode,
        String sepayId,
        String transactionDate,  // ISO-8601
        String paymentMethod,    // vd "Chuyển khoản ngân hàng (SePay)"

        // tài khoản nhận tiền (bên bán)
        String bankName,
        String bankAccountNo,
        String bankAccountName,

        // hiệu lực đăng ký
        String startDate,        // ISO date
        String endDate           // ISO date
) {
    public static final String TOPIC = "billing.payment.success";
}
