package com.insightflow.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
@Slf4j
public class JwtValidator {

    private final SecretKey secretKey;
    private final long clockSkewSeconds;

    public JwtValidator(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.clock-skew-seconds:30}") long clockSkewSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.clockSkewSeconds = clockSkewSeconds;
    }

    /**
     * Validates signature, expiry, and required claims (sub, tenant_id).
     *
     * @throws JwtException on any validation failure
     */
    public Claims validate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .clockSkewSeconds(clockSkewSeconds)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new JwtException("Missing required claim: sub");
        }
        if (claims.get("tenant_id") == null) {
            throw new JwtException("Missing required claim: tenant_id");
        }
        return claims;
    }
}
