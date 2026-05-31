package com.insightflow.auth.controller;

import com.insightflow.auth.dto.request.ForgotPasswordRequest;
import com.insightflow.auth.dto.request.LoginRequest;
import com.insightflow.auth.dto.request.LogoutRequest;
import com.insightflow.auth.dto.request.RefreshRequest;
import com.insightflow.auth.dto.request.RegisterTenantRequest;
import com.insightflow.auth.dto.request.ResetPasswordRequest;
import com.insightflow.auth.dto.request.UpdateProfileRequest;
import com.insightflow.auth.dto.response.AuthResponse;
import com.insightflow.auth.dto.response.MessageResponse;
import com.insightflow.auth.dto.response.TenantRegistrationResult;
import com.insightflow.auth.dto.response.UserInfo;
import com.insightflow.auth.service.AuthService;
import com.insightflow.auth.service.PasswordResetService;
import com.insightflow.auth.service.TenantService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
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
@Tag(name = "Authentication", description = "Tenant onboarding and token management")
public class AuthController {

    private final TenantService tenantService;
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register-tenant")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new tenant with an owner account")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tenant and owner created"),
        @ApiResponse(responseCode = "409", description = "Tenant slug already taken"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public TenantRegistrationResult registerTenant(@Valid @RequestBody RegisterTenantRequest request) {
        return tenantService.registerTenant(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive access + refresh tokens")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue new access token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens rotated"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke refresh token")
    @ApiResponse(responseCode = "204", description = "Token revoked")
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info (requires gateway JWT validation)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User info returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    public UserInfo me(@CurrentUser UserContext user) {
        return authService.getMe(user.userId());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Request password reset email")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Reset email sent if account exists"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String message = passwordResetService.initiateReset(request.getEmail());
        return MessageResponse.builder().message(message).build();
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Reset password using token from email")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successful"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return MessageResponse.builder().message("Password has been reset successfully.").build();
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile settings",
               description = "Updates onboarding settings (location, categories, businessScale, platforms, profileComplete) stored in tenant settings JSON.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    public ResponseEntity<UserInfo> updateMe(
            @CurrentUser UserContext user,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserInfo userInfo = authService.updateMe(user.userId(), user.tenantId(), request);
        return ResponseEntity.ok(userInfo);
    }
}
