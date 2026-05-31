package com.insightflow.recommendation.ai;

public interface FashionAiClient {

    /**
     * Gọi AI phân tích chiến lược tồn kho và dự báo xu hướng
     * @param inventoryData Chuỗi dữ liệu tồn kho được nén
     * @param completenessScore Điểm chất lượng dữ liệu
     * @param missingFields Các trường bị thiếu
     * @return Chuỗi JSON kết quả từ AI
     */
    String generateInventoryStrategy(String inventoryData, Double completenessScore, String missingFields);

    /**
     * Xác định nhà cung cấp AI (gemini, openai...)
     */
    String getProviderName();
}