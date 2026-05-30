package com.insightflow.auth.service;

import com.insightflow.auth.dto.request.ChangePasswordRequest;
import com.insightflow.auth.dto.request.LoginRequest;
import com.insightflow.auth.dto.request.LogoutRequest;
import com.insightflow.auth.dto.request.RefreshTokenRequest;
import com.insightflow.auth.dto.request.RegisterTenantRequest;
import com.insightflow.auth.dto.response.TokenResponse;
import com.insightflow.auth.dto.response.UserResponse;

public interface AuthService {

    TokenResponse registerTenant(RegisterTenantRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    UserResponse me();

    void changePassword(ChangePasswordRequest request);
}
