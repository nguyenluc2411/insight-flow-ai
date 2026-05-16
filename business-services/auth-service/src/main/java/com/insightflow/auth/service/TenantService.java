package com.insightflow.auth.service;

import com.insightflow.auth.dto.request.RegisterTenantRequest;
import com.insightflow.auth.dto.response.AuthResponse;
import com.insightflow.auth.dto.response.TenantInfo;
import com.insightflow.auth.dto.response.TenantRegistrationResult;
import com.insightflow.auth.dto.response.UserInfo;
import com.insightflow.auth.entity.RefreshToken;
import com.insightflow.auth.entity.Role;
import com.insightflow.auth.entity.Tenant;
import com.insightflow.auth.entity.User;
import com.insightflow.auth.event.TenantRegisteredEvent;
import com.insightflow.auth.exception.ConflictException;
import com.insightflow.auth.repository.RefreshTokenRepository;
import com.insightflow.auth.repository.RoleRepository;
import com.insightflow.auth.repository.TenantRepository;
import com.insightflow.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordService passwordService;
    private final TokenHashService tokenHashService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.jwt.refresh-expiration-days:30}")
    private long refreshExpirationDays;

    @Transactional
    public TenantRegistrationResult registerTenant(RegisterTenantRequest request) {
        if (tenantRepository.existsBySlug(request.getSlug())) {
            throw new ConflictException("Tenant slug '" + request.getSlug() + "' is already taken");
        }

        Tenant tenant = Tenant.builder()
                .name(request.getTenantName())
                .slug(request.getSlug())
                .plan("trial")
                .status("active")
                .trialEndsAt(Instant.now().plus(14, ChronoUnit.DAYS))
                .build();
        tenant = tenantRepository.save(tenant);

        List<Role> ownerRoles = roleRepository.findByTenantIdIsNullAndNameIn(List.of("OWNER"));
        if (ownerRoles.isEmpty()) {
            log.error("OWNER role not found in DB — was V4 migration applied?");
        }

        User owner = User.builder()
                .tenantId(tenant.getId())
                .email(request.getOwnerEmail().toLowerCase().strip())
                .passwordHash(passwordService.encode(request.getOwnerPassword()))
                .fullName(request.getOwnerFullName())
                .status("active")
                .roles(Set.copyOf(ownerRoles))
                .build();
        owner = userRepository.save(owner);

        String rawRefreshToken = jwtService.generateRefreshToken();
        RefreshToken storedToken = RefreshToken.builder()
                .userId(owner.getId())
                .tokenHash(tokenHashService.hash(rawRefreshToken))
                .expiresAt(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(storedToken);

        String accessToken = jwtService.issueAccessToken(owner, tenant);

        publishTenantRegistered(tenant, owner);

        log.info("Tenant registered slug={} ownerId={}", tenant.getSlug(), owner.getId());

        return TenantRegistrationResult.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresIn(900L)
                .user(toUserInfo(owner, tenant))
                .tenant(TenantInfo.builder()
                        .id(tenant.getId())
                        .name(tenant.getName())
                        .slug(tenant.getSlug())
                        .plan(tenant.getPlan())
                        .trialEndsAt(tenant.getTrialEndsAt())
                        .build())
                .build();
    }

    private void publishTenantRegistered(Tenant tenant, User owner) {
        TenantRegisteredEvent event = TenantRegisteredEvent.builder()
                .tenantId(tenant.getId().toString())
                .tenantSlug(tenant.getSlug())
                .plan(tenant.getPlan())
                .ownerId(owner.getId().toString())
                .build();
        try {
            kafkaTemplate.send("auth.tenant.registered", tenant.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish TenantRegisteredEvent tenantId={}: {}", tenant.getId(), e.getMessage());
        }
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
