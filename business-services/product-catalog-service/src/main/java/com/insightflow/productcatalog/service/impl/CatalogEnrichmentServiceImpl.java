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

import java.text.Normalizer;
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

    @Override
    public Map<String, String> resolveColumns(List<String> headers) {
        Map<String, String> result = new HashMap<>();
        if (headers == null || headers.isEmpty()) return result;

        Map<String, String> colDict = internalCache.get("COLUMN_MAPPING");
        if (colDict == null || colDict.isEmpty()) {
            log.warn("⚠️ [COLUMN] Từ điển COLUMN_MAPPING rỗng (chưa seed/refresh?) — trả về map rỗng.");
            return result;
        }

        // Chỉ mục dạng "nén" (bỏ dấu '_') để gộp các biến thể viết liền/gạch dưới/cách:
        // "mau_sac" == "mausac" == "mau sac" đều quy về một entry.
        Map<String, String> compactDict = new HashMap<>();
        for (Map.Entry<String, String> e : colDict.entrySet()) {
            compactDict.putIfAbsent(e.getKey().replace("_", ""), e.getValue());
        }

        for (String header : headers) {
            if (header == null || header.isBlank()) continue;
            String norm = normalizeColumnKey(header);

            String field = colDict.get(norm);                            // 1) khớp chính xác
            if (field == null) field = compactDict.get(norm.replace("_", "")); // 2) khớp dạng nén
            if (field == null) field = fuzzyColumn(norm, colDict);       // 3) fuzzy cho lỗi gõ

            if (field != null) {
                result.put(header, field);
            } else {
                log.info("ℹ️ [COLUMN] Chưa nhận diện được cột '{}' (norm='{}')", header, norm);
            }
        }
        log.info("🧭 [COLUMN] Nhận diện {}/{} cột.", result.size(), headers.size());
        return result;
    }

    /** Fuzzy Levenshtein trên dạng nén; bỏ qua chuỗi quá ngắn để tránh khớp nhầm. */
    private String fuzzyColumn(String norm, Map<String, String> colDict) {
        String compact = norm.replace("_", "");
        if (compact.length() < 4) return null;

        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Map.Entry<String, String> e : colDict.entrySet()) {
            String synCompact = e.getKey().replace("_", "");
            // Bỏ synonym quá ngắn (ma, co, sl, qty...): fuzzy với chúng dễ khớp nhầm
            // (vd "mota"/Mô tả chỉ lệch 2 ký tự so với "ma" -> nhận vơ vào product_code).
            if (synCompact.length() < 4) continue;
            // Ngưỡng theo độ dài: chuỗi ngắn chỉ cho lệch 1; đủ dài (>=6) mới cho lệch 2.
            int maxDist = Math.min(synCompact.length(), compact.length()) >= 6 ? MAX_DISTANCE_THRESHOLD : 1;
            int distance = levenshtein.apply(compact, synCompact);
            if (distance > 0 && distance <= maxDist && distance < bestDist) {
                bestDist = distance;
                best = e.getValue();
            }
        }
        if (best != null) {
            log.info("🎯 [COLUMN FUZZY] '{}' (dist={}) -> '{}'", norm, bestDist, best);
        }
        return best;
    }

    /** Chuẩn hóa header về cùng dạng DynamicFileParser: thường, khử dấu, đ->d, snake_case. */
    private String normalizeColumnKey(String raw) {
        String t = raw.toLowerCase().trim();
        t = Normalizer.normalize(t, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        t = t.replace("đ", "d");
        t = t.replaceAll("[^a-z0-9_\\s]", "");
        return t.replaceAll("\\s+", "_");
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