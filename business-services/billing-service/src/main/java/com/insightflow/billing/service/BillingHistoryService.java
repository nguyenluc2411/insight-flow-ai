package com.insightflow.billing.service;

import com.insightflow.billing.entity.BillingHistory;
import com.insightflow.billing.repository.BillingHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingHistoryService {

    private final BillingHistoryRepository billingHistoryRepository;

    @Transactional(readOnly = true)
    public Page<BillingHistory> getHistory(UUID tenantId, Pageable pageable) {
        return billingHistoryRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Transactional
    public BillingHistory recordEvent(UUID tenantId, UUID subscriptionId, String eventType,
                                      String fromPackageCode, String toPackageCode,
                                      Integer amountVnd, String description) {
        BillingHistory history = BillingHistory.builder()
                .tenantId(tenantId)
                .subscriptionId(subscriptionId)
                .eventType(eventType)
                .fromPackageCode(fromPackageCode)
                .toPackageCode(toPackageCode)
                .amountVnd(amountVnd)
                .description(description)
                .build();
        return billingHistoryRepository.save(history);
    }
}
