package com.insightflow.auth.service;

import com.insightflow.auth.dto.request.*;
import com.insightflow.auth.dto.response.TokenResponse;
import com.insightflow.auth.dto.response.UserResponse;
import com.insightflow.auth.entity.*;
import com.insightflow.auth.exception.AuthException;
import com.insightflow.auth.mapper.UserMapper;
import com.insightflow.auth.repository.*;
import com.insightflow.common.security.TenantContextHolder;
import com.insightflow.common.web.exception.ConflictException;
import com.insightflow.common.web.exception.ErrorCode;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    // ── Register Tenant ───────────────────────────────────────────────────────

    @Override
    public TokenResponse registerTenant(RegisterTenantRequest request) {
        if (tenantRepository.existsBySlug(request.getTenantSlug())) {
            throw new ConflictException("Tenant slug already taken: " + request.getTenantSlug());
        }

        Tenant tenant = Tenant.builder()
                .name(request.getTenantName())
                .slug(request.getTenantSlug())
                .plan("trial")
                .status("active")
                .build();
        tenant = tenantRepository.save(tenant);

        User user = User.builder()
                .tenantId(tenant.getId())
                .email(request.getEmail().toLowerCase().strip())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status("active")
                .build();
        user = userRepository.save(user);

        Role ownerRole = roleRepository.findByName("OWNER")
                .orElseThrow(() -> new IllegalStateException("OWNER role missing — check seed migration V6"));

        userRoleRepository.save(UserRole.builder()
                .userId(user.getId())
                .roleId(ownerRole.getId())
                .tenantId(tenant.getId())
                .build());

        log.info("Tenant registered: slug={} ownerId={}", tenant.getSlug(), user.getId());
        return buildTokenResponse(user, tenant, List.of("OWNER"), null);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    public TokenResponse login(LoginRequest request) {
        Tenant tenant = tenantRepository.findBySlug(request.getTenantSlug())
                .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED, "Invalid credentials"));

        if (!"active".equals(tenant.getStatus())) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "Invalid credentials");
        }

        User user = userRepository.findByTenantIdAndEmail(tenant.getId(), request.getEmail().toLowerCase().strip())
                .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "Invalid credentials");
        }

        if (!"active".equals(user.getStatus())) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "Invalid credentials");
        }

        List<String> roleNames = userRoleRepository.findRoleNamesByUserIdAndTenantId(user.getId(), tenant.getId());
        log.debug("User logged in: userId={} tenantSlug={}", user.getId(), tenant.getSlug());
        return buildTokenResponse(user, tenant, roleNames, null);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Override
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken oldToken = tokenService.rotateRefreshToken(request.getRefreshToken());

        User user = userRepository.findById(oldToken.getUserId())
                .orElseThrow(() -> new AuthException(ErrorCode.TOKEN_INVALID, "User not found"));
        Tenant tenant = tenantRepository.findById(oldToken.getTenantId())
                .orElseThrow(() -> new AuthException(ErrorCode.TOKEN_INVALID, "Tenant not found"));

        if (!"active".equals(user.getStatus()) || !"active".equals(tenant.getStatus())) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "Account is inactive");
        }

        List<String> roleNames = userRoleRepository.findRoleNamesByUserIdAndTenantId(user.getId(), tenant.getId());
        return buildTokenResponse(user, tenant, roleNames, oldToken.getDeviceFingerprint());
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Override
    public void logout(LogoutRequest request) {
        tokenService.revokeRefreshToken(request.getRefreshToken());
    }

    // ── Me ────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserResponse me() {
        UUID userId = TenantContextHolder.requireUserId();
        UUID tenantId = TenantContextHolder.requireTenantId();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<String> roles = userRoleRepository.findRoleNamesByUserIdAndTenantId(userId, tenantId);
        return userMapper.toResponse(user, roles);
    }

    // ── Change Password ───────────────────────────────────────────────────────

    @Override
    public void changePassword(ChangePasswordRequest request) {
        UUID userId = TenantContextHolder.requireUserId();
        UUID tenantId = TenantContextHolder.requireTenantId();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens so all other sessions are invalidated
        tokenService.revokeAllForUser(userId);
        log.info("Password changed for userId={}", userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TokenResponse buildTokenResponse(User user, Tenant tenant,
                                             List<String> roleNames, String deviceFingerprint) {
        List<UUID> roleIds = userRoleRepository
                .findAllByUserIdAndTenantId(user.getId(), tenant.getId())
                .stream().map(UserRole::getRoleId).toList();

        List<String> permissions = roleRepository.findAllByIdIn(roleIds)
                .stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(com.insightflow.auth.entity.Permission::getName)
                .distinct()
                .sorted()
                .toList();

        String accessToken = tokenService.generateAccessToken(
                user, tenant.getSlug(), tenant.getPlan(), roleNames, permissions);

        String rawRefreshToken = tokenService.createRefreshToken(
                user.getId(), tenant.getId(), deviceFingerprint);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .expiresIn(tokenService.accessTokenTtlSeconds())
                .refreshToken(rawRefreshToken)
                .build();
    }
}
