package com.insightflow.auth.controller;

import com.insightflow.auth.dto.request.AssignRolesRequest;
import com.insightflow.auth.dto.request.CreateUserRequest;
import com.insightflow.auth.dto.request.UpdateUserRequest;
import com.insightflow.auth.dto.response.RoleResponse;
import com.insightflow.auth.dto.response.UserResponse;
import com.insightflow.auth.service.RoleService;
import com.insightflow.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// NOTE: The api-gateway currently routes /api/v1/auth/** to auth-service.
// /api/v1/users/** and /api/v1/roles/** need a dedicated route added by gateway-agent.
// Until then, these endpoints are accessible directly on port 8081.
@RestController
@RequiredArgsConstructor
@Tag(name = "Users & Roles", description = "User management and role listing within a tenant")
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    // ── User management ───────────────────────────────────────────────────────

    @PostMapping("/api/v1/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create user in the caller's tenant (OWNER or MANAGER only)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "409", description = "Email already exists in tenant")
    })
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping("/api/v1/users")
    @Operation(summary = "List users in the caller's tenant (paginated, OWNER or MANAGER)")
    @ApiResponse(responseCode = "200", description = "Page of users")
    public Page<UserResponse> listUsers(@PageableDefault(size = 20) Pageable pageable) {
        return userService.listUsers(pageable);
    }

    @GetMapping("/api/v1/users/{id}")
    @Operation(summary = "Get user by ID within the caller's tenant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public UserResponse getUser(@PathVariable UUID id) {
        return userService.getUser(id);
    }

    @PatchMapping("/api/v1/users/{id}")
    @Operation(summary = "Update user name or status (OWNER or MANAGER)")
    @ApiResponse(responseCode = "200", description = "User updated")
    public UserResponse updateUser(@PathVariable UUID id,
                                   @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/api/v1/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate user (OWNER only, cannot deactivate self)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deactivated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "422", description = "Cannot deactivate own account")
    })
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/v1/users/{id}/roles")
    @Operation(summary = "Replace all roles for a user (OWNER only)")
    @ApiResponse(responseCode = "200", description = "Roles replaced")
    public UserResponse assignRoles(@PathVariable UUID id,
                                    @Valid @RequestBody AssignRolesRequest request) {
        return userService.assignRoles(id, request);
    }

    // ── Role listing ──────────────────────────────────────────────────────────

    @GetMapping("/api/v1/roles")
    @Operation(summary = "List all system roles with their permissions")
    @ApiResponse(responseCode = "200", description = "List of roles")
    public List<RoleResponse> listRoles() {
        return roleService.listAllRoles();
    }
}
