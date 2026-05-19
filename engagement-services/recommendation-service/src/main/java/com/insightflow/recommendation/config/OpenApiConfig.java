package com.insightflow.recommendation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI recommendationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Recommendation Service API")
                        .version("1.0.0")
                        .description("Recommendation insights and analytics"));
    }
}
