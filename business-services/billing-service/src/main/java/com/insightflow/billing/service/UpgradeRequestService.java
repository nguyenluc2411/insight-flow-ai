package com.insightflow.billing.service;

import com.insightflow.billing.dto.request.UpgradeRequest;
import com.insightflow.billing.dto.response.UpgradeRequestResponse;
import com.insightflow.billing.entity.BillingPackage;
import com.insightflow.billing.entity.Plan;
import com.insightflow.billing.entity.PlanUpgradeRequest;
import com.insightflow.billing.repository.PackageRepository;
import com.insightflow.billing.repository.PlanRepository;
import com.insightflow.billing.repository.PlanUpgradeRequestRepository;
import com.insightflow.common.web.exception.BusinessException;
import com.insightflow.common.web.exception.ErrorCode;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Manual upgrade flow (no payment gateway in MVP):
 * user submits a request -> admin/ops approves (service JWT) -> plan switched.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpgradeRequestService {

    private final PlanUpgradeRequestRepository requestRepository;
    private final SubscriptionService subscriptionService;
    private final PackageRepository packageRepository;
    private final PlanRepository planRepository;

    @Transactional
    public UpgradeRequestResponse createRequest(UUID tenantId, String packageCode, String billingCycle) {
        BillingPackage pkg = packageRepository.findByCodeAndStatus(packageCode, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Package not found or inactive: " + packageCode));
        String cycle = (billingCycle == null || billingCycle.isBlank()) ? "MONTHLY" : billingCycle.toUpperCase();

        PlanUpgradeRequest req = PlanUpgradeRequest.builder()
                .tenantId(tenantId)
                .requestedPackageCode(pkg.getCode())
                .billingCycle(cycle)
                .status("PENDING")
                .build();
        PlanUpgradeRequest saved = requestRepository.save(req);
        log.info("Tenant [{}] requested upgrade to [{}] ({})", tenantId, pkg.getCode(), cycle);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UpgradeRequestResponse> listByStatus(String status) {
        return requestRepository.findByStatusOrderByCreatedAtAsc(status.toUpperCase())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public UpgradeRequestResponse approve(UUID requestId) {
        PlanUpgradeRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Upgrade request not found: " + requestId));
        if (!"PENDING".equals(req.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Request already " + req.getStatus());
        }

        BillingPackage pkg = packageRepository.findByCode(req.getRequestedPackageCode())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + req.getRequestedPackageCode()));
        Plan plan = planRepository.findByPackageIdAndBillingCycle(pkg.getId(), req.getBillingCycle())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plan not found for " + pkg.getCode() + "/" + req.getBillingCycle()));

        UpgradeRequest dto = new UpgradeRequest();
        dto.setPlanId(plan.getId());
        dto.setBillingCycle(plan.getBillingCycle());
        dto.setAutoRenew(true);
        subscriptionService.upgradePlan(req.getTenantId(), dto);

        req.setStatus("APPROVED");
        req.setResolvedAt(Instant.now());
        log.info("Upgrade request [{}] approved — tenant [{}] -> [{}]", requestId, req.getTenantId(), pkg.getCode());
        return toResponse(requestRepository.save(req));
    }

    @Transactional
    public UpgradeRequestResponse reject(UUID requestId, String reason) {
        PlanUpgradeRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Upgrade request not found: " + requestId));
        if (!"PENDING".equals(req.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Request already " + req.getStatus());
        }
        req.setStatus("REJECTED");
        req.setNote(reason);
        req.setResolvedAt(Instant.now());
        log.info("Upgrade request [{}] rejected", requestId);
        return toResponse(requestRepository.save(req));
    }

    private UpgradeRequestResponse toResponse(PlanUpgradeRequest r) {
        return UpgradeRequestResponse.builder()
                .id(r.getId())
                .tenantId(r.getTenantId())
                .requestedPackageCode(r.getRequestedPackageCode())
                .billingCycle(r.getBillingCycle())
                .status(r.getStatus())
                .note(r.getNote())
                .resolvedAt(r.getResolvedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
