package com.insightflow.auth.service;

import com.insightflow.auth.dto.request.AssignRolesRequest;
import com.insightflow.auth.dto.request.CreateUserRequest;
import com.insightflow.auth.dto.request.UpdateUserRequest;
import com.insightflow.auth.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    // ── Create User ───────────────────────────────────────────────────────────

    UserResponse createUser(CreateUserRequest request);

    // ── List Users ────────────────────────────────────────────────────────────

    Page<UserResponse> listUsers(Pageable pageable);

    // ── Get User ──────────────────────────────────────────────────────────────

    UserResponse getUser(UUID userId);

    // ── Update User ───────────────────────────────────────────────────────────

    UserResponse updateUser(UUID userId, UpdateUserRequest request);

    // ── Deactivate User ───────────────────────────────────────────────────────

    void deactivateUser(UUID userId);

    // ── Assign Roles ──────────────────────────────────────────────────────────

    UserResponse assignRoles(UUID userId, AssignRolesRequest request);
}
