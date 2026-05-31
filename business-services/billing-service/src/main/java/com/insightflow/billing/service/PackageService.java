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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
