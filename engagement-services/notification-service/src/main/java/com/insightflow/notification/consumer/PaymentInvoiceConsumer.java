package com.insightflow.notification.consumer;

import com.insightflow.common.events.billing.PaymentReceiptEvent;
import com.insightflow.notification.service.email.InvoiceEmailService;
import com.insightflow.notification.service.interfaces.ProcessedEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Tiêu thụ sự kiện {@code billing.payment.success} (biên nhận thanh toán) do billing-service phát,
 * rồi gửi email biên nhận cho khách. Idempotent qua {@link ProcessedEventService} để không gửi trùng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentInvoiceConsumer {

    private final ProcessedEventService processedEventService;
    private final InvoiceEmailService invoiceEmailService;

    @KafkaListener(
            topics = PaymentReceiptEvent.TOPIC,
            containerFactory = "paymentReceiptKafkaListenerContainerFactory")
    public void onPaymentSuccess(
            PaymentReceiptEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        if (event == null || event.eventId() == null) {
            log.warn("[INVOICE-CONSUMER] Bỏ qua event rỗng/thiếu eventId topic={} offset={}", topic, offset);
            return;
        }

        UUID eventId = toUuid(event.eventId());
        boolean first = processedEventService.recordIfNotProcessed(
                eventId,
                "billing.payment.success",
                null,
                "billing-service",
                Instant.now(),
                event,
                topic);

        if (!first) {
            log.info("[INVOICE-CONSUMER] Bỏ qua biên nhận trùng invoice={} eventId={} offset={}",
                    event.invoiceNumber(), event.eventId(), offset);
            return;
        }

        if (event.recipientEmail() == null || event.recipientEmail().isBlank()) {
            log.warn("[INVOICE-CONSUMER] Biên nhận {} không có email người nhận (tenant={}) — bỏ qua gửi mail.",
                    event.invoiceNumber(), event.tenantId());
            return;
        }

        log.info("[INVOICE-CONSUMER] Xử lý biên nhận invoice={} tenant={} email={} offset={}",
                event.invoiceNumber(), event.tenantId(), event.recipientEmail(), offset);

        invoiceEmailService.sendReceipt(event);
    }

    /** eventId từ billing là UUID dạng chuỗi; nếu không parse được thì suy ra UUID ổn định từ chuỗi. */
    private UUID toUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
        }
    }
}
