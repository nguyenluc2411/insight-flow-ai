package com.insightflow.auth.service;

import com.insightflow.auth.entity.Tenant;
import com.insightflow.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    private final String issuer;
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expiration-minutes:15}") long expirationMinutes) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Issues a signed access token.
     * Claim structure must match api-gateway JwtValidator exactly.
     */
    public String issueAccessToken(User user, Tenant tenant) {
        List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName())
                .toList();

        List<String> permissionNames = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getResource() + ":" + p.getAction())
                .distinct()
                .toList();

        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(issuer)
                .claim("tenant_id", tenant.getId().toString())
                .claim("tenant_slug", tenant.getSlug())
                .claim("plan", tenant.getPlan())
                .claim("roles", roleNames)
                .claim("permissions", permissionNames)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Issues a random opaque refresh token (UUID).
     * Store only the hash — never the raw value.
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
