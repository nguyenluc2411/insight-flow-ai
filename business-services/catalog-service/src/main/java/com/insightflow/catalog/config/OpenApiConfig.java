package com.insightflow.catalog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI catalogOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Catalog Service API")
                        .version("1.0.0")
                        .description("Products, variants, locations, and inventory management"));
    }
}
