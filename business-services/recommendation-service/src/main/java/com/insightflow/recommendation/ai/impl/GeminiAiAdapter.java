package com.insightflow.recommendation.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.insightflow.recommendation.ai.FashionAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service("geminiAiClient")
public class GeminiAiAdapter implements FashionAiClient {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;

    public GeminiAiAdapter(RestTemplate restTemplate,
                           @Value("${ai.gemini.url}") String apiUrl,
                           @Value("${ai.gemini.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public String generateInventoryStrategy(String inventoryData, Double completenessScore, String missingFields) {
        log.info("🤖 Đang gọi Google Gemini Pro xử lý phân tích chiến lược doanh thu...");

        double score = completenessScore != null ? completenessScore : 0.0;
        String missingStr = (missingFields != null && !missingFields.isEmpty()) ? missingFields : "[]";

        // 1. Dựng Prompt Tích hợp Điểm Chất lượng Data
        String systemPrompt = "Bạn là một Chuyên gia phân tích dữ liệu bán lẻ và Chiến lược gia chuỗi cung ứng thời trang.\n" +
                "Điểm chất lượng dữ liệu: " + score + " (Nếu < 0.5, hãy cảnh báo gay gắt).\n" +
                "Các trường dữ liệu bị thiếu: " + missingStr + "\n" +
                "Dữ liệu báo cáo hàng tồn kho:\n" + inventoryData + "\n" +
                "Nhiệm vụ của bạn là phân tích dữ liệu trên để đưa ra chiến lược tối ưu dòng vốn và dự báo xu hướng.\n" +
                "RÀNG BUỘC TUYỆT ĐỐI: CHỈ trả về KẾT QUẢ DUY NHẤT LÀ MỘT CHUỖI JSON (không dùng markdown block, không giải thích), đúng cấu trúc sau:\n" +
                "{\n" +
                "  \"inventory_strategy\": [\n" +
                "    { \"item_id_or_category\": \"...\", \"issue\": \"Tồn kho cao/Chậm luân chuyển\", \"action\": \"Xả hàng\", \"discount_percentage_recommendation\": 30, \"target_channel\": \"Sàn TMĐT/Cửa hàng Outlet\", \"reasoning\": \"...\" }\n" +
                "  ],\n" +
                "  \"trend_forecasting\": [\n" +
                "    { \"suggested_item\": \"Tên mặt hàng hot\", \"relevance_to_current_inventory\": \"...\", \"estimated_import_quantity\": 150, \"expected_retail_price_range\": \"...\", \"market_trend_reasoning\": \"...\" }\n" +
                "  ]\n" +
                "}";

        // 2. Gói Data Format + Safety Settings (Chống chặn API)
        String requestBody = "{\n" +
                "  \"contents\": [{\"parts\":[{\"text\": \"" + systemPrompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"}]}],\n" +
                "  \"generationConfig\": {\"responseMimeType\": \"application/json\"},\n" +
                "  \"safetySettings\": [\n" +
                "    {\"category\": \"HARM_CATEGORY_SEXUALLY_EXPLICIT\", \"threshold\": \"BLOCK_NONE\"},\n" +
                "    {\"category\": \"HARM_CATEGORY_HARASSMENT\", \"threshold\": \"BLOCK_NONE\"}\n" +
                "  ]\n" +
                "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        String url = apiUrl + "?key=" + apiKey;

        try {
            // 3. Bóp cò gọi API (Sử dụng RestTemplate đồng nhất)
            JsonNode response = restTemplate.postForObject(url, entity, JsonNode.class);

            if (response == null || !response.has("candidates")) {
                throw new RuntimeException("API AI trả về kết quả không hợp lệ hoặc bị chặn.");
            }

            String rawText = response.at("/candidates/0/content/parts/0/text").asText();

            // 4. Bóc tách JSON bằng Regex thép
            return extractCleanJson(rawText);

        } catch (Exception e) {
            log.error("❌ Lỗi luồng kết nối API Gemini: {}", e.getMessage());
            throw e; // Bắt buộc ném lỗi để Annotation @Retryable tự động kích hoạt gọi lại
        }
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    /**
     * Bóc tách JSON cực mạnh: lách lỗi markdown cắt code của UI
     */
    private String extractCleanJson(String rawOutput) {
        if (rawOutput == null || rawOutput.trim().isEmpty()) {
            throw new RuntimeException("AI trả về kết quả rỗng");
        }

        String clean = rawOutput.trim();

        // Regex sử dụng mã Unicode để tìm block JSON an toàn
        String regexStr = "\\u0060\\u0060\\u0060(?:json)?\\s*(\\{.*\\}|\\[.*\\])\\s*\\u0060\\u0060\\u0060";
        Matcher matcher = Pattern.compile(regexStr, Pattern.DOTALL).matcher(clean);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        int startIndex = clean.indexOf('{');
        int endIndex = clean.lastIndexOf('}');
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return clean.substring(startIndex, endIndex + 1);
        }

        return clean; // Fallback cuối cùng
    }
}