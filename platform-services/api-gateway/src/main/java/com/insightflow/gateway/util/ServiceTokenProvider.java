package com.insightflow.gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Issues short-lived service-to-service JWTs for calling billing-service internal
 * endpoints. The token is signed with the shared {@code app.service-jwt.secret} that
 * billing-service's ServiceJwtValidator verifies. Cached in memory and refreshed
 * shortly before expiry to avoid signing on every request.
 */
@Component
public class ServiceTokenProvider {

    private static final long TTL_HOURS = 1;
    private static final long REFRESH_SKEW_SECONDS = 60;

    private final SecretKey signingKey;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public ServiceTokenProvider(@Value("${app.service-jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public synchronized String getToken() {
        Instant now = Instant.now();
        if (cachedToken == null || now.isAfter(expiresAt.minusSeconds(REFRESH_SKEW_SECONDS))) {
            Instant exp = now.plus(TTL_HOURS, ChronoUnit.HOURS);
            cachedToken = Jwts.builder()
                    .subject("api-gateway")
                    .issuer("insightflow-gateway")
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(exp))
                    .signWith(signingKey)
                    .compact();
            expiresAt = exp;
        }
        return cachedToken;
    }
}
