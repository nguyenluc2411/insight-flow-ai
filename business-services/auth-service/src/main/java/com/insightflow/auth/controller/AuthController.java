package com.insightflow.auth.controller;

import com.insightflow.auth.dto.request.*;
import com.insightflow.auth.dto.response.TokenResponse;
import com.insightflow.auth.dto.response.UserResponse;
import com.insightflow.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and session management")
public class AuthController {

    private final AuthService authService;

    // ── Public endpoints ──────────────────────────────────────────────────────

    @PostMapping("/register-tenant")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register new tenant and owner account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tenant created, tokens returned"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Tenant slug already taken")
    })
    public ResponseEntity<TokenResponse> registerTenant(@Valid @RequestBody RegisterTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerTenant(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email, password, and tenant slug")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, tokens returned"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and get a new token pair")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New token pair returned"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalid or expired")
    })
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    // ── Protected endpoints (require JWT via gateway) ─────────────────────────

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke refresh token (logout)")
    @ApiResponse(responseCode = "204", description = "Logged out")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public UserResponse me() {
        return authService.me();
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change current user password (invalidates all other sessions)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed"),
            @ApiResponse(responseCode = "401", description = "Current password is incorrect")
    })
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.noContent().build();
    }
}
