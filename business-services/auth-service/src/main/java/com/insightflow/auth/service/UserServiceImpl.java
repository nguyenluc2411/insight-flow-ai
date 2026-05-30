package com.insightflow.auth.service;

import com.insightflow.auth.dto.request.AssignRolesRequest;
import com.insightflow.auth.dto.request.CreateUserRequest;
import com.insightflow.auth.dto.request.UpdateUserRequest;
import com.insightflow.auth.dto.response.UserResponse;
import com.insightflow.auth.entity.Role;
import com.insightflow.auth.entity.User;
import com.insightflow.auth.entity.UserRole;
import com.insightflow.auth.exception.AuthException;
import com.insightflow.auth.mapper.UserMapper;
import com.insightflow.auth.repository.RoleRepository;
import com.insightflow.auth.repository.UserRepository;
import com.insightflow.auth.repository.UserRoleRepository;
import com.insightflow.common.security.TenantContextHolder;
import com.insightflow.common.web.exception.ConflictException;
import com.insightflow.common.web.exception.ErrorCode;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import com.insightflow.common.web.exception.TenantAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private static final Set<String> MANAGER_ROLES = Set.of("OWNER", "MANAGER");
    private static final Set<String> OWNER_ONLY = Set.of("OWNER");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    // ── Create User ───────────────────────────────────────────────────────────

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        requireRole(MANAGER_ROLES);

        String email = request.getEmail().toLowerCase().strip();
        if (userRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new ConflictException("User with this email already exists in the tenant");
        }

        List<Role> roles = resolveRoles(request.getRoles());

        User user = User.builder()
                .tenantId(tenantId)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status("active")
                .build();
        user = userRepository.save(user);

        assignRolesToUser(user.getId(), tenantId, roles);
        log.info("User created: userId={} tenantId={}", user.getId(), tenantId);
        return userMapper.toResponse(user, request.getRoles());
    }

    // ── List Users ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        requireRole(MANAGER_ROLES);

        return userRepository.findAllByTenantId(tenantId, pageable)
                .map(user -> {
                    List<String> roles = userRoleRepository.findRoleNamesByUserIdAndTenantId(user.getId(), tenantId);
                    return userMapper.toResponse(user, roles);
                });
    }

    // ── Get User ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        requireRole(MANAGER_ROLES);

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<String> roles = userRoleRepository.findRoleNamesByUserIdAndTenantId(userId, tenantId);
        return userMapper.toResponse(user, roles);
    }

    // ── Update User ───────────────────────────────────────────────────────────

    @Override
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        requireRole(MANAGER_ROLES);

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)  user.setLastName(request.getLastName());
        if (request.getStatus() != null)    user.setStatus(request.getStatus());

        userRepository.save(user);
        List<String> roles = userRoleRepository.findRoleNamesByUserIdAndTenantId(userId, tenantId);
        return userMapper.toResponse(user, roles);
    }

    // ── Deactivate User ───────────────────────────────────────────────────────

    @Override
    public void deactivateUser(UUID userId) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        UUID callerId = TenantContextHolder.requireUserId();
        requireRole(OWNER_ONLY);

        if (userId.equals(callerId)) {
            throw new AuthException(ErrorCode.BUSINESS_RULE_VIOLATION, "Cannot deactivate your own account");
        }

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setStatus("inactive");
        userRepository.save(user);
        log.info("User deactivated: userId={} tenantId={}", userId, tenantId);
    }

    // ── Assign Roles ──────────────────────────────────────────────────────────

    @Override
    public UserResponse assignRoles(UUID userId, AssignRolesRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        requireRole(OWNER_ONLY);

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<Role> roles = resolveRoles(request.getRoles());

        // Replace all roles for this user in this tenant
        userRoleRepository.deleteByUserIdAndTenantId(userId, tenantId);
        assignRolesToUser(userId, tenantId, roles);

        return userMapper.toResponse(user, request.getRoles());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void requireRole(Set<String> allowed) {
        UUID userId = TenantContextHolder.requireUserId();
        UUID tenantId = TenantContextHolder.requireTenantId();
        List<String> callerRoles = userRoleRepository.findRoleNamesByUserIdAndTenantId(userId, tenantId);
        boolean hasRole = callerRoles.stream().anyMatch(allowed::contains);
        if (!hasRole) {
            throw new TenantAccessException("Insufficient role. Required: " + allowed);
        }
    }

    private List<Role> resolveRoles(List<String> roleNames) {
        return roleNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", name)))
                .toList();
    }

    private void assignRolesToUser(UUID userId, UUID tenantId, List<Role> roles) {
        roles.forEach(role -> userRoleRepository.save(UserRole.builder()
                .userId(userId)
                .roleId(role.getId())
                .tenantId(tenantId)
                .build()));
    }
}
