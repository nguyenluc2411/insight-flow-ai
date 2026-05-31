package com.insightflow.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantRegistrationResult {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private UserInfo user;
    private TenantInfo tenant;
}
