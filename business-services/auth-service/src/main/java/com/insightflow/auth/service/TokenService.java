package com.insightflow.auth.service;

import com.insightflow.auth.entity.RefreshToken;
import com.insightflow.auth.entity.User;

import java.util.List;
import java.util.UUID;

public interface TokenService {

    String generateAccessToken(User user, String tenantSlug, String plan,
                               List<String> roles, List<String> permissions);

    long accessTokenTtlSeconds();

    String createRefreshToken(UUID userId, UUID tenantId, String deviceFingerprint);

    RefreshToken rotateRefreshToken(String rawToken);

    void revokeRefreshToken(String rawToken);

    void revokeAllForUser(UUID userId);
}
