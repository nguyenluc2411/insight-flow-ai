package com.insightflow.recommendation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
    // Bean này giải quyết lỗi "Could not autowire. No beans of 'RestTemplate' type found"
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}