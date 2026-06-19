package com.insightflow.billing.service;

import com.insightflow.billing.dto.response.BillingMetricsResponse;
import com.insightflow.billing.entity.BillingPackage;
import com.insightflow.billing.entity.PaymentTransaction;
import com.insightflow.billing.entity.Plan;
import com.insightflow.billing.entity.TenantSubscription;
import com.insightflow.billing.repository.PackageRepository;
import com.insightflow.billing.repository.PaymentTransactionRepository;
import com.insightflow.billing.repository.PlanRepository;
import com.insightflow.billing.repository.TenantSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Aggregate revenue/subscription metrics for the super-admin dashboard. */
@Service
@RequiredArgsConstructor
public class BillingMetricsService {

    private static final String STATUS_SUCCESS = "SUCCESS";

    private final TenantSubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final PackageRepository packageRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Transactional(readOnly = true)
    public BillingMetricsResponse getMetrics(int months) {
        int window = Math.max(1, Math.min(months, 36));

        List<TenantSubscription> active = subscriptionRepository.findByStatus("ACTIVE");
        List<TenantSubscription> trial = subscriptionRepository.findByStatus("TRIAL");

        long mrr = active.stream()
                .map(TenantSubscription::getPriceAtSubscription)
                .filter(p -> p != null)
                .mapToLong(Integer::longValue)
                .sum();

        // planId -> package display name (resolve via plan -> package).
        Map<UUID, UUID> planToPackage = planRepository.findAll().stream()
                .collect(Collectors.toMap(Plan::getId, Plan::getPackageId, (a, b) -> a));
        Map<UUID, String> packageName = packageRepository.findAll().stream()
                .collect(Collectors.toMap(BillingPackage::getId, BillingPackage::getName, (a, b) -> a));

        Map<String, Long> byPlan = new LinkedHashMap<>();
        for (TenantSubscription sub : concat(active, trial)) {
            String name = packageName.getOrDefault(planToPackage.get(sub.getPlanId()), "Unknown");
            byPlan.merge(name, 1L, Long::sum);
        }

        // Revenue: successful payments grouped by calendar month.
        List<PaymentTransaction> paid = paymentTransactionRepository.findByStatus(STATUS_SUCCESS);
        long totalRevenue = paid.stream()
                .map(PaymentTransaction::getAmount)
                .filter(a -> a != null)
                .mapToLong(Integer::longValue)
                .sum();

        Map<YearMonth, Long> revenueMap = new LinkedHashMap<>();
        YearMonth current = YearMonth.from(LocalDate.now(ZoneOffset.UTC));
        for (int i = window - 1; i >= 0; i--) {
            revenueMap.put(current.minusMonths(i), 0L);
        }
        for (PaymentTransaction tx : paid) {
            if (tx.getCreatedAt() == null || tx.getAmount() == null) continue;
            YearMonth ym = YearMonth.from(tx.getCreatedAt());
            revenueMap.computeIfPresent(ym, (k, v) -> v + tx.getAmount());
        }
        List<BillingMetricsResponse.MonthlyRevenue> revenueByMonth = revenueMap.entrySet().stream()
                .map(e -> new BillingMetricsResponse.MonthlyRevenue(e.getKey().toString(), e.getValue()))
                .toList();

        return new BillingMetricsResponse(
                active.size(), trial.size(), mrr, totalRevenue, byPlan, revenueByMonth);
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        return java.util.stream.Stream.concat(a.stream(), b.stream()).toList();
    }
}
