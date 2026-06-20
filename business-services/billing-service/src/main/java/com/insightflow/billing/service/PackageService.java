package com.insightflow.billing.service;

import com.insightflow.billing.dto.response.PackageResponse;
import com.insightflow.billing.dto.response.PlanResponse;
import com.insightflow.billing.entity.BillingPackage;
import com.insightflow.billing.entity.Feature;
import com.insightflow.billing.entity.PackageFeature;
import com.insightflow.billing.entity.Plan;
import com.insightflow.billing.repository.FeatureRepository;
import com.insightflow.billing.repository.PackageRepository;
import com.insightflow.billing.repository.PlanRepository;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.insightflow.billing.repository.PackageFeatureRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PackageService {

    private final PackageRepository packageRepository;
    private final PlanRepository planRepository;
    private final FeatureRepository featureRepository;
    private final PackageFeatureRepository packageFeatureRepository;

    public List<PackageResponse> getAllActivePackages() {
        List<BillingPackage> packages = packageRepository.findByStatusOrderByDisplayOrderAsc("ACTIVE");

        // Build feature code map per package
        List<PackageFeature> allPackageFeatures = packageFeatureRepository.findAll();
        List<Feature> allFeatures = featureRepository.findAll();
        Map<UUID, String> featureCodeMap = allFeatures.stream()
                .collect(Collectors.toMap(Feature::getId, Feature::getCode));

        Map<UUID, List<String>> packageFeatureCodesMap = allPackageFeatures.stream()
                .collect(Collectors.groupingBy(
                        PackageFeature::getPackageId,
                        Collectors.mapping(pf -> featureCodeMap.getOrDefault(pf.getFeatureId(), ""), Collectors.toList())
                ));

        return packages.stream()
                .map(pkg -> {
                    List<Plan> plans = planRepository.findByPackageIdAndStatus(pkg.getId(), "ACTIVE");
                    List<String> featureCodes = packageFeatureCodesMap.getOrDefault(pkg.getId(), List.of());
                    return toPackageResponse(pkg, plans, featureCodes);
                })
                .collect(Collectors.toList());
    }

    // ─── Platform super-admin catalog management ──────────────────────────────

    /** All packages (any status, including hidden/INACTIVE) with all their plans. */
    public List<PackageResponse> getAllPackagesAdmin() {
        List<BillingPackage> packages = packageRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        p -> p.getDisplayOrder() == null ? Integer.MAX_VALUE : p.getDisplayOrder()))
                .toList();

        List<PackageFeature> allPackageFeatures = packageFeatureRepository.findAll();
        Map<UUID, String> featureCodeMap = featureRepository.findAll().stream()
                .collect(Collectors.toMap(Feature::getId, Feature::getCode));
        Map<UUID, List<String>> packageFeatureCodesMap = allPackageFeatures.stream()
                .collect(Collectors.groupingBy(
                        PackageFeature::getPackageId,
                        Collectors.mapping(pf -> featureCodeMap.getOrDefault(pf.getFeatureId(), ""), Collectors.toList())
                ));

        return packages.stream()
                .map(pkg -> toPackageResponse(
                        pkg,
                        planRepository.findByPackageId(pkg.getId()),
                        packageFeatureCodesMap.getOrDefault(pkg.getId(), List.of())))
                .collect(Collectors.toList());
    }

    /** Update a plan's price / trial / cycle / status. Null fields are left unchanged. */
    @Transactional
    public PlanResponse updatePlan(UUID planId, Integer priceVnd, Integer trialDays,
                                   String billingCycle, String status) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));
        if (priceVnd != null) {
            if (priceVnd < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "priceVnd must be >= 0");
            plan.setPriceVnd(priceVnd);
        }
        if (trialDays != null) {
            if (trialDays < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "trialDays must be >= 0");
            plan.setTrialDays(trialDays);
        }
        if (billingCycle != null) plan.setBillingCycle(billingCycle);
        if (status != null) plan.setStatus(status.toUpperCase());
        log.info("Admin updated plan {} -> price={}, trialDays={}, cycle={}, status={}",
                planId, plan.getPriceVnd(), plan.getTrialDays(), plan.getBillingCycle(), plan.getStatus());
        return toPlanResponse(planRepository.save(plan));
    }

    /** Update package metadata / visibility. Null fields are left unchanged. */
    @Transactional
    public PackageResponse updatePackage(UUID id, String name, String description,
                                         Integer displayOrder, String status) {
        BillingPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + id));
        if (name != null) pkg.setName(name);
        if (description != null) pkg.setDescription(description);
        if (displayOrder != null) pkg.setDisplayOrder(displayOrder);
        if (status != null) pkg.setStatus(status.toUpperCase());
        packageRepository.save(pkg);
        return toPackageResponse(pkg, planRepository.findByPackageId(id), getFeatureCodesForPackage(id));
    }

    /** Create a new package together with an initial monthly plan. */
    @Transactional
    public PackageResponse createPackage(String code, String name, String description,
                                         Integer displayOrder, Integer monthlyPriceVnd, Integer trialDays) {
        String normalizedCode = code == null ? null : code.trim().toUpperCase();
        if (normalizedCode == null || normalizedCode.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code is required");
        if (name == null || name.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        packageRepository.findByCode(normalizedCode).ifPresent(p -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Package code already exists: " + normalizedCode);
        });

        BillingPackage pkg = packageRepository.save(BillingPackage.builder()
                .code(normalizedCode)
                .name(name)
                .description(description)
                .displayOrder(displayOrder)
                .status("ACTIVE")
                .version(1)
                .build());

        Plan plan = planRepository.save(Plan.builder()
                .packageId(pkg.getId())
                .billingCycle("MONTHLY")
                .priceVnd(monthlyPriceVnd == null ? 0 : monthlyPriceVnd)
                .currency("VND")
                .trialDays(trialDays == null ? 0 : trialDays)
                .status("ACTIVE")
                .build());

        log.info("Admin created package {} ({}) with monthly plan price={}", normalizedCode, pkg.getId(), plan.getPriceVnd());
        return toPackageResponse(pkg, List.of(plan), List.of());
    }

    public PackageResponse getPackageByCode(String code) {
        BillingPackage pkg = packageRepository.findByCodeAndStatus(code, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + code));
        List<Plan> plans = planRepository.findByPackageIdAndStatus(pkg.getId(), "ACTIVE");
        List<String> featureCodes = getFeatureCodesForPackage(pkg.getId());
        return toPackageResponse(pkg, plans, featureCodes);
    }

    public List<String> getFeatureCodesForPackage(UUID packageId) {
        List<PackageFeature> packageFeatures = packageFeatureRepository.findByPackageId(packageId);
        List<UUID> featureIds = packageFeatures.stream()
                .map(PackageFeature::getFeatureId)
                .collect(Collectors.toList());
        if (featureIds.isEmpty()) return List.of();
        return featureRepository.findAllById(featureIds).stream()
                .map(Feature::getCode)
                .collect(Collectors.toList());
    }

    private PackageResponse toPackageResponse(BillingPackage pkg, List<Plan> plans, List<String> featureCodes) {
        return PackageResponse.builder()
                .id(pkg.getId())
                .code(pkg.getCode())
                .version(pkg.getVersion())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .displayOrder(pkg.getDisplayOrder())
                .status(pkg.getStatus())
                .plans(plans.stream().map(this::toPlanResponse).collect(Collectors.toList()))
                .featureCodes(featureCodes)
                .build();
    }

    private PlanResponse toPlanResponse(Plan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .packageId(plan.getPackageId())
                .billingCycle(plan.getBillingCycle())
                .priceVnd(plan.getPriceVnd())
                .currency(plan.getCurrency())
                .trialDays(plan.getTrialDays())
                .status(plan.getStatus())
                .build();
    }
}
