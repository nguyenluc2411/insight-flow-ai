package com.insightflow.recommendation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.services")
public class ServiceProperties {

    // Spring Boot sẽ tự map biến này với key "app.services.data-ingestion-url" trong file yml
    private String dataIngestionUrl;


}