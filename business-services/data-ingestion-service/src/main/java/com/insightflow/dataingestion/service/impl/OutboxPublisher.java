package com.insightflow.dataingestion.service.impl;

import com.insightflow.dataingestion.entity.OutboxEvent;
import com.insightflow.dataingestion.messaging.ReliableKafkaProducer;
import com.insightflow.dataingestion.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepo;
    private final ReliableKafkaProducer kafkaProducer;

    // Quét mỗi 5 giây, lấy tối đa 100 record để tránh OutOfMemory
    @Scheduled(fixedDelayString = "${app.outbox.poll-interval:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepo.findByPublishedFalseOrderByCreatedAtAsc(Pageable.ofSize(100));

        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            processSingleEvent(event);
        }
    }

    // REQUIRES_NEW: Đảm bảo nếu 1 event lỗi DB lúc update, nó KHÔNG kéo theo các event khác bị rollback
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleEvent(OutboxEvent event) {
        try {
            // Tên topic được lấy động từ eventType, không hardcode!
            kafkaProducer.publish(event.getEventType(), event.getPayload());

            event.setPublished(true);
            event.setPublishedAt(LocalDateTime.now());
            outboxRepo.save(event);

        } catch (Exception e) {
            log.error("🔥 [OUTBOX ERROR] Failed to process event ID [{}]: {}", event.getId(), e.getMessage());
            // Bỏ qua event này, vòng lặp sau sẽ thử lại (Retry cơ bản)
        }
    }
}