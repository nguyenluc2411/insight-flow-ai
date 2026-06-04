package com.insightflow.auth.service;

import com.insightflow.auth.dto.request.LoginRequest;
import com.insightflow.auth.dto.request.LogoutRequest;
import com.insightflow.auth.dto.request.RefreshRequest;
import com.insightflow.auth.dto.request.UpdateProfileRequest;
import com.insightflow.auth.dto.response.AuthResponse;
import com.insightflow.auth.dto.response.UserInfo;
import com.insightflow.auth.entity.RefreshToken;
import com.insightflow.auth.entity.Role;
import com.insightflow.auth.entity.Tenant;
import com.insightflow.auth.entity.User;
import com.insightflow.common.web.exception.UnauthorizedException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        // Email is globally unique → identifies the user (and thus the tenant) on its own.
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().strip())
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));

        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));

        if (!passwordService.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        if (!"active".equals(user.getStatus())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return buildAuthResponse(user, tenant);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = tokenHashService.hash(request.getRefreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        if (!stored.isActive()) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new UnauthorizedException("Tenant not found"));

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
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new UnauthorizedException("Tenant not found"));
        return toUserInfo(user, tenant);
    }

    @Transactional
    public UserInfo updateMe(UUID userId, UUID tenantId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new UnauthorizedException("Tenant not found"));

        Map<String, Object> settings = new HashMap<>(
                tenant.getSettings() != null ? tenant.getSettings() : Map.of());
        if (request.getLocation() != null)      settings.put("location", request.getLocation());
        if (request.getCategories() != null)    settings.put("categories", request.getCategories());
        if (request.getBusinessScale() != null) settings.put("businessScale", request.getBusinessScale());
        if (request.getPlatforms() != null)     settings.put("platforms", request.getPlatforms());
        if (request.getProfileComplete() != null) settings.put("profileComplete", request.getProfileComplete());

        tenant.setSettings(settings);
        tenantRepository.save(tenant);

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

    @SuppressWarnings("unchecked")
    private UserInfo toUserInfo(User user, Tenant tenant) {
        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        Map<String, Object> s = tenant.getSettings() != null ? tenant.getSettings() : Map.of();
        return UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .tenantId(tenant.getId())
                .tenantSlug(tenant.getSlug())
                .roles(roleNames)
                .location((String) s.get("location"))
                .categories((List<String>) s.get("categories"))
                .businessScale((String) s.get("businessScale"))
                .platforms((List<String>) s.get("platforms"))
                .profileComplete((Boolean) s.get("profileComplete"))
                .build();
    }
}
