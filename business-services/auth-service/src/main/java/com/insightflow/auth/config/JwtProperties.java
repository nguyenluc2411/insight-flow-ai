package com.insightflow.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;
    private String issuer = "insightflow-auth-service";
    private long accessTokenTtlSeconds = 900;
    private int refreshTokenTtlDays = 30;
}
