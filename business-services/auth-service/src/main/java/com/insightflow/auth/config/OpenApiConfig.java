package com.insightflow.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private int serverPort;

    @Bean
    public OpenAPI authServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .description("Tenant onboarding, user authentication, and JWT token management")
                        .version("v1")
                        .contact(new Contact()
                                .name("Insight Flow AI")
                                .email("dev@insightflow.ai")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local dev"),
                        new Server().url("http://localhost:8080").description("Via API Gateway (dev)")
                ));
    }
}
