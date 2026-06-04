package com.insightflow.productcatalog.service.impl;

import com.insightflow.productcatalog.dto.request.EnrichmentRequest;
import com.insightflow.productcatalog.dto.response.EnrichmentResponse;
import com.insightflow.productcatalog.entity.CatalogDictionary;
import com.insightflow.productcatalog.repository.CatalogDictionaryRepository;
import com.insightflow.productcatalog.service.CatalogEnrichmentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogEnrichmentServiceImpl implements CatalogEnrichmentService {

    private final CatalogDictionaryRepository dictionaryRepository;
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    private static final int MAX_DISTANCE_THRESHOLD = 2;
    private final Map<String, Map<String, String>> internalCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initCache() {
        refreshCache();
    }

    @Override
    public synchronized void refreshCache() {
        log.info("📡 [CACHE] Đang tải từ điển dữ liệu từ Database lên RAM Cache...");
        List<CatalogDictionary> allRules = dictionaryRepository.findAll();

        internalCache.clear();

        for (CatalogDictionary rule : allRules) {
            String type = rule.getAttributeType().toUpperCase();
            String synonym = rule.getSynonym().toLowerCase().trim();
            String standard = rule.getStandardValue();

            internalCache.computeIfAbsent(type, k -> new ConcurrentHashMap<>()).put(synonym, standard);
        }
        log.info("✅ [CACHE] Tải từ điển thành công! Đã nạp {} loại thuộc tính vào RAM.", internalCache.size());
    }

    @Override
    public EnrichmentResponse enrichProductData(EnrichmentRequest request) {
        String nameText = request.getProductName() != null ? request.getProductName().toLowerCase() : "";
        String catText = request.getRawCategory() != null ? request.getRawCategory().toLowerCase() : "";
        String colorText = request.getRawColor() != null ? request.getRawColor().toLowerCase() : "";

        String fullContextText = (nameText + " " + catText).replaceAll("\\s+", " ").trim();

        // Xóa sạch mớ if-else kia đi, chỉ giữ lại đúng 1 dòng này:
        String demographic = resolveValue("TARGET_DEMOGRAPHIC", fullContextText, "Unisex");

        String department = resolveValue("DEPARTMENT", fullContextText, "Apparel");
        String category = resolveValue("CATEGORY", fullContextText, "Unknown");
        String subCategory = resolveValue("SUB_CATEGORY", fullContextText, "Unknown");
        String material = resolveValue("MATERIAL", fullContextText, null);
        String colorFamily = resolveValue("COLOR", colorText, "Unknown");

        Map<String, String> extractedAttributes = new HashMap<>();

        String fitType = resolveValue("FIT_TYPE", fullContextText, null);
        if (fitType != null) extractedAttributes.put("fit_type", fitType);

        String neckline = resolveValue("NECKLINE", fullContextText, null);
        if (neckline != null) extractedAttributes.put("neckline", neckline);

        String sleeveLength = resolveValue("SLEEVE_LENGTH", fullContextText, null);
        if (sleeveLength != null) extractedAttributes.put("sleeve_length", sleeveLength);

        return EnrichmentResponse.builder()
                .department(department)
                .category(category)
                .subCategory(subCategory)
                .targetDemographic(demographic)
                .material(material)
                .colorFamily(colorFamily)
                .extractedAttributes(extractedAttributes)
                .build();
    }

    private String resolveValue(String attributeType, String inputText, String defaultValue) {
        Map<String, String> subMap = internalCache.get(attributeType.toUpperCase());
        if (subMap == null || inputText.isEmpty()) {
            return defaultValue;
        }

        String bestMatch = null;
        int longestLength = 0;

        for (Map.Entry<String, String> entry : subMap.entrySet()) {
            String synonym = entry.getKey();
            if (inputText.contains(synonym) && synonym.length() > longestLength) {
                bestMatch = entry.getValue();
                longestLength = synonym.length();
            }
        }
        if (bestMatch != null) return bestMatch;

        String[] words = inputText.split("\\s+");
        for (String word : words) {
            if (word.length() < 3) continue;

            for (Map.Entry<String, String> entry : subMap.entrySet()) {
                String synonym = entry.getKey();

                if (!synonym.contains(" ")) {
                    int distance = levenshtein.apply(word, synonym);
                    if (distance > 0 && distance <= MAX_DISTANCE_THRESHOLD) {
                        log.info("🎯 [FUZZY MATCHED] Đã nhận diện lỗi chính tả: '{}' -> Nghĩa chuẩn: '{}' (Loại: {})", word, synonym, attributeType);
                        return entry.getValue();
                    }
                }
            }
        }

        return defaultValue;
    }
}