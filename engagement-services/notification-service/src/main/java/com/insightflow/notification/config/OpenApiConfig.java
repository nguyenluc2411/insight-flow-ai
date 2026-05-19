package com.insightflow.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .description("In-app and email notifications driven by Kafka events.")
                        .version("v1"))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Via Gateway (dev)"),
                        new Server().url("http://localhost:8091").description("Direct (dev)")
                ));
    }
}
