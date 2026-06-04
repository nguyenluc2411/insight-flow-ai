package com.insightflow.auth.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.auth.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingEventListener {

    private final ObjectMapper objectMapper;
    private final TenantRepository tenantRepository;

    @KafkaListener(topics = "billing.subscription.changed", groupId = "auth-service-events")
    @Transactional
    public void onSubscriptionChanged(ConsumerRecord<String, String> record) {
        try {
            JsonNode payload = objectMapper.readTree(record.value());


            String tenantIdStr = payload.path("tenantId").asText(null);
            String newPackageCode = payload.path("newPackageCode").asText(null);

            if (tenantIdStr == null || newPackageCode == null) {
                log.error("❌ [AUTH] Dữ liệu từ Billing bị thiếu trường bắt buộc. Payload: {}. BỎ QUA!", record.value());
                return;
            }


            UUID tenantId;
            try {
                tenantId = UUID.fromString(tenantIdStr);
            } catch (IllegalArgumentException ex) {
                log.error("❌ [AUTH] Sai định dạng UUID: {}. BỎ QUA!", tenantIdStr);
                return;
            }

            log.info("🎧 [AUTH] Nhận tin báo cập nhật gói cước: Tenant {} -> {}", tenantId, newPackageCode);


            tenantRepository.findById(tenantId).ifPresentOrElse(
                    tenant -> {
                        tenant.setPlan(newPackageCode.toLowerCase());
                        tenantRepository.save(tenant);
                        log.info("✅ [AUTH] Đã cập nhật gói cước thành công cho Tenant: {}", tenantId);
                    },
                    () -> {
                        log.warn("⚠️ [AUTH] Không tìm thấy Tenant {} trong DB để nâng cấp. BỎ QUA.", tenantId);
                    }
            );

        } catch (DataIntegrityViolationException dbEx) {
            log.error("❌ [AUTH] Lỗi DB (Có thể do sai tên gói cước). Vui lòng check lại CHECK CONSTRAINT. BỎ QUA!");


        } catch (Exception e) {

            log.error("💥 [AUTH] Lỗi hệ thống khi xử lý Kafka event, sẽ tự động Retry: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi hệ thống khi xử lý event billing.subscription.changed", e);
        }
    }
}