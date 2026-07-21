package com.insightflow.billing.event.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.billing.entity.TenantContact;
import com.insightflow.billing.entity.TenantUserCount;
import com.insightflow.billing.repository.TenantContactRepository;
import com.insightflow.billing.repository.TenantUserCountRepository;
import com.insightflow.billing.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventListener {

    private final TenantUserCountRepository tenantUserCountRepository;
    private final TenantContactRepository tenantContactRepository;
    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "auth.tenant.registered", groupId = "billing-service-events")
    @Transactional
    public void onTenantRegistered(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode payload = objectMapper.readTree(record.value());
            // TenantRegisteredEvent uses snake_case serialization (tenant_id).
            JsonNode idNode = payload.hasNonNull("tenant_id") ? payload.get("tenant_id") : payload.get("tenantId");
            UUID tenantId = UUID.fromString(idNode.asText());
            subscriptionService.createTrialSubscription(tenantId);

            // Cache email/tên chủ tenant để sau này gửi hóa đơn/biên nhận thanh toán
            // (event mang sẵn owner_email + owner_name — trước đây bị bỏ qua).
            upsertContact(tenantId,
                    textOrNull(payload, "owner_email", "ownerEmail"),
                    textOrNull(payload, "owner_name", "ownerName"));
        } catch (Exception e) {
            log.error("Failed to process auth.tenant.registered event: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }

    private void upsertContact(UUID tenantId, String email, String name) {
        if (email == null || email.isBlank()) {
            log.warn("auth.tenant.registered thiếu owner email cho tenant={} — bỏ qua cache contact", tenantId);
            return;
        }
        TenantContact contact = tenantContactRepository.findById(tenantId)
                .orElseGet(() -> TenantContact.builder().tenantId(tenantId).build());
        contact.setEmail(email.toLowerCase().strip());
        if (name != null && !name.isBlank()) {
            contact.setName(name.strip());
        }
        tenantContactRepository.save(contact);
        log.info("Đã cache liên hệ tenant={} email={}", tenantId, contact.getEmail());
    }

    private String textOrNull(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.hasNonNull(key)) {
                return node.get(key).asText();
            }
        }
        return null;
    }

    @KafkaListener(topics = "auth.user.created", groupId = "billing-service-events")
    @Transactional
    public void onUserCreated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode payload = objectMapper.readTree(record.value());
            UUID tenantId = UUID.fromString(payload.get("tenantId").asText());

            int updated = tenantUserCountRepository.incrementUserCount(tenantId);
            if (updated == 0) {
                // No record exists — create one
                tenantUserCountRepository.save(TenantUserCount.builder()
                        .tenantId(tenantId)
                        .userCount(1)
                        .build());
            }
            log.debug("Incremented user count for tenantId={}", tenantId);
        } catch (Exception e) {
            log.error("Failed to process auth.user.created event: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "auth.user.deleted", groupId = "billing-service-events")
    @Transactional
    public void onUserDeleted(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode payload = objectMapper.readTree(record.value());
            UUID tenantId = UUID.fromString(payload.get("tenantId").asText());

            tenantUserCountRepository.decrementUserCount(tenantId);
            log.debug("Decremented user count for tenantId={}", tenantId);
        } catch (Exception e) {
            log.error("Failed to process auth.user.deleted event: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }
}
