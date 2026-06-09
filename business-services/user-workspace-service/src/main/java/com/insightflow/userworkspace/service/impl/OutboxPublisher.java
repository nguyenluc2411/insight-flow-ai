package com.insightflow.userworkspace.service.impl;


import com.insightflow.userworkspace.entity.OutboxEvent;
import com.insightflow.userworkspace.messaging.ReliableKafkaProducer;
import com.insightflow.userworkspace.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepo;
    private final ReliableKafkaProducer kafkaProducer;

    @Scheduled(fixedDelayString = "5000")
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepo.findByPublishedFalseOrderByCreatedAtAsc(Pageable.ofSize(100));
        for (OutboxEvent event : pendingEvents) {
            processSingleEvent(event);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleEvent(OutboxEvent event) {
        try {
            kafkaProducer.publish(event.getEventType(), event.getPayload());
            event.setPublished(true);
            event.setPublishedAt(LocalDateTime.now());
            outboxRepo.save(event);
        } catch (Exception e) {
            log.error("🔥 [OUTBOX] Lỗi xử lý event ID [{}]: {}", event.getId(), e.getMessage());
        }
    }
}