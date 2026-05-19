package com.insightflow.bff.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dashboardBffOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dashboard BFF API")
                        .description("Backend-for-Frontend — aggregates catalog, sales, and ml-service APIs for the Insight Flow dashboard.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Insight Flow Team")
                                .email("dev@insightflow.ai")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Via Gateway (dev)"),
                        new Server().url("http://localhost:8090").description("Direct (dev)")
                ));
    }
}
