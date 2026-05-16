package com.insightflow.auth.service;

import com.insightflow.auth.dto.request.LoginRequest;
import com.insightflow.auth.dto.request.LogoutRequest;
import com.insightflow.auth.dto.request.RefreshRequest;
import com.insightflow.auth.dto.response.AuthResponse;
import com.insightflow.auth.dto.response.UserInfo;
import com.insightflow.auth.entity.RefreshToken;
import com.insightflow.auth.entity.Role;
import com.insightflow.auth.entity.Tenant;
import com.insightflow.auth.entity.User;
import com.insightflow.auth.exception.AuthException;
import com.insightflow.auth.repository.RefreshTokenRepository;
import com.insightflow.auth.repository.TenantRepository;
import com.insightflow.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordService passwordService;
    private final TokenHashService tokenHashService;

    @Value("${app.jwt.refresh-expiration-days:30}")
    private long refreshExpirationDays;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Tenant tenant = tenantRepository.findBySlug(request.getTenantSlug())
                .orElseThrow(() -> new AuthException(INVALID_CREDENTIALS));

        User user = userRepository.findByTenantIdAndEmail(tenant.getId(),
                        request.getEmail().toLowerCase().strip())
                .orElseThrow(() -> new AuthException(INVALID_CREDENTIALS));

        if (!passwordService.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException(INVALID_CREDENTIALS);
        }

        if (!"active".equals(user.getStatus())) {
            throw new AuthException(INVALID_CREDENTIALS);
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return buildAuthResponse(user, tenant);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = tokenHashService.hash(request.getRefreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash)
                .orElseThrow(() -> new AuthException("Invalid or expired refresh token"));

        if (!stored.isActive()) {
            throw new AuthException("Invalid or expired refresh token");
        }

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new AuthException("User not found"));

        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new AuthException("Tenant not found"));

        return buildAuthResponse(user, tenant);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String hash = tokenHashService.hash(request.getRefreshToken());
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash)
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                });
    }

    public UserInfo getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));
        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new AuthException("Tenant not found"));
        return toUserInfo(user, tenant);
    }

    private AuthResponse buildAuthResponse(User user, Tenant tenant) {
        String rawRefresh = jwtService.generateRefreshToken();
        RefreshToken stored = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHashService.hash(rawRefresh))
                .expiresAt(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(stored);

        String accessToken = jwtService.issueAccessToken(user, tenant);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefresh)
                .expiresIn(900L)
                .user(toUserInfo(user, tenant))
                .build();
    }

    private UserInfo toUserInfo(User user, Tenant tenant) {
        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        return UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .tenantId(tenant.getId())
                .tenantSlug(tenant.getSlug())
                .roles(roleNames)
                .build();
    }
}
