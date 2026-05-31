package com.insightflow.gateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "Insight Flow AI — API Gateway",
        version     = "1.0",
        description = "Aggregated API documentation. Obtain a Bearer token from POST /api/v1/auth/login, then click Authorize."
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name        = "bearerAuth",
    type        = SecuritySchemeType.HTTP,
    scheme      = "bearer",
    bearerFormat = "JWT",
    in          = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {}
