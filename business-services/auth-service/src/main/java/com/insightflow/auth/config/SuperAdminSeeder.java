package com.insightflow.auth.config;

import com.insightflow.auth.entity.Role;
import com.insightflow.auth.entity.Tenant;
import com.insightflow.auth.entity.User;
import com.insightflow.auth.repository.RoleRepository;
import com.insightflow.auth.repository.TenantRepository;
import com.insightflow.auth.repository.UserRepository;
import com.insightflow.auth.service.PasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Ensures a platform super-admin account exists on startup.
 *
 * <p>Idempotent: skips when the admin already exists, so a password changed
 * after first login is never overwritten. Credentials come from env
 * (ADMIN_EMAIL / ADMIN_PASSWORD) — never hard-coded in a migration.</p>
 *
 * <p>The admin lives in a dedicated "platform" tenant because users.tenant_id
 * is NOT NULL; this tenant is excluded from admin tenant listings.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class SuperAdminSeeder implements ApplicationRunner {

    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordService passwordService;

    @Value("${app.admin.email:admin@insightflow.id.vn}")
    private String adminEmail;

    @Value("${app.admin.password:InsightFlow@2026}")
    private String adminPassword;

    @Value("${app.admin.full-name:Platform Admin}")
    private String adminFullName;

    @Value("${app.admin.platform-tenant-slug:platform}")
    private String platformSlug;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = adminEmail.toLowerCase().strip();

        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Super-admin already present ({}) — skipping seed", email);
            return;
        }

        List<Role> superRoles = roleRepository.findByTenantIdIsNullAndNameIn(List.of(SUPER_ADMIN_ROLE));
        if (superRoles.isEmpty()) {
            log.error("{} role not found — was V8 migration applied? Skipping admin seed.", SUPER_ADMIN_ROLE);
            return;
        }

        Tenant platform = tenantRepository.findBySlug(platformSlug)
                .orElseGet(() -> tenantRepository.save(Tenant.builder()
                        .name("InsightFlow Platform")
                        .slug(platformSlug)
                        .plan("pro")
                        .status("active")
                        .build()));

        User admin = User.builder()
                .tenantId(platform.getId())
                .email(email)
                .passwordHash(passwordService.encode(adminPassword))
                .fullName(adminFullName)
                .status("active")
                .roles(Set.copyOf(superRoles))
                .build();
        userRepository.save(admin);

        log.info("Seeded super-admin account: {} (platform tenant '{}')", email, platformSlug);
    }
}
