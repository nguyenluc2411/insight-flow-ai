package com.insightflow.auth.service;

import com.insightflow.auth.config.JwtProperties;
import com.insightflow.auth.entity.RefreshToken;
import com.insightflow.auth.entity.User;
import com.insightflow.auth.exception.AuthException;
import com.insightflow.auth.repository.RefreshTokenRepository;
import com.insightflow.common.security.SecurityConstants;
import com.insightflow.common.web.exception.ErrorCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    // ── Access token ──────────────────────────────────────────────────────────

    @Override
    public String generateAccessToken(User user, String tenantSlug, String plan,
                                      List<String> roles, List<String> permissions) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.getAccessTokenTtlSeconds());

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(SecurityConstants.CLAIM_TENANT_ID, user.getTenantId().toString())
                .claim(SecurityConstants.CLAIM_TENANT_SLUG, tenantSlug)
                .claim(SecurityConstants.CLAIM_PLAN, plan)
                .claim(SecurityConstants.CLAIM_ROLES, roles)
                .claim(SecurityConstants.CLAIM_PERMISSIONS, permissions)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }

    @Override
    public long accessTokenTtlSeconds() {
        return jwtProperties.getAccessTokenTtlSeconds();
    }

    // ── Refresh token ─────────────────────────────────────────────────────────

    /**
     * Creates a new refresh token, stores only its SHA-256 hash, and returns
     * the raw (plaintext) token to be given to the client.
     */
    @Override
    @Transactional
    public String createRefreshToken(UUID userId, UUID tenantId, String deviceFingerprint) {
        // Revoke existing token for same device so only one is active per device
        if (StringUtils.hasText(deviceFingerprint)) {
            refreshTokenRepository.revokeByUserIdAndFingerprint(userId, deviceFingerprint, Instant.now());
        }

        String rawToken = UUID.randomUUID().toString();
        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tenantId(tenantId)
                .tokenHash(hashToken(rawToken))
                .deviceFingerprint(deviceFingerprint)
                .expiresAt(Instant.now().plus(jwtProperties.getRefreshTokenTtlDays(), ChronoUnit.DAYS))
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Validates the raw refresh token, revokes it (rotate-on-use), and returns
     * the stored entity so the caller can issue new tokens.
     */
    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(String rawToken) {
        String hash = hashToken(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AuthException(ErrorCode.TOKEN_INVALID, "Invalid refresh token"));

        if (!token.isValid()) {
            throw new AuthException(ErrorCode.TOKEN_EXPIRED, "Refresh token has expired or been revoked");
        }

        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
        return token;
    }

    /** Revoke the specific refresh token on logout. */
    @Override
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        String hash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    /** Revoke all refresh tokens for a user (e.g. on password change). */
    @Override
    @Transactional
    public void revokeAllForUser(UUID userId) {
        int count = refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        log.debug("Revoked {} refresh tokens for userId={}", count, userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
