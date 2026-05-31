package com.insightflow.billing.service;

import com.insightflow.billing.entity.OutboxEvent;
import com.insightflow.billing.event.producer.BillingKafkaProducer;
import com.insightflow.billing.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepo;
    private final BillingKafkaProducer kafkaProducer;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxRepo.findByPublishedFalseOrderByCreatedAtAsc(Pageable.ofSize(100));

        for (OutboxEvent event : pending) {
            try {
                kafkaProducer.publish("billing." + event.getEventType(), event.getPayload());
                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());
                outboxRepo.save(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event [{}] id=[{}]: {}", event.getEventType(), event.getId(), e.getMessage());
            }
        }
    }
}
