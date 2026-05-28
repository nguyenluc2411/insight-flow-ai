package com.insightflow.auth.service;

import com.insightflow.auth.entity.PasswordResetToken;
import com.insightflow.auth.repository.PasswordResetTokenRepository;
import com.insightflow.auth.repository.RefreshTokenRepository;
import com.insightflow.auth.repository.UserRepository;
import com.insightflow.common.web.exception.BusinessException;
import com.insightflow.common.web.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final String RESET_REQUESTED_MSG =
            "If that email is registered, a reset link has been sent.";
    private static final String INVALID_TOKEN_MSG = "Invalid or expired reset token";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenHashService tokenHashService;
    private final PasswordService passwordService;
    private final EmailService emailService;

    @Value("${app.reset-password.token-expiry-hours:1}")
    private long tokenExpiryHours;

    @Transactional
    public String initiateReset(String email) {
        String normalizedEmail = email.toLowerCase().strip();

        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            // Clean up any existing unused tokens for this user
            passwordResetTokenRepository.deleteByUserId(user.getId());

            String rawToken = UUID.randomUUID().toString();
            String tokenHash = tokenHashService.hash(rawToken);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .userId(user.getId())
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().plus(tokenExpiryHours, ChronoUnit.HOURS))
                    .build();

            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(normalizedEmail, rawToken);
            log.info("Password reset initiated for user id={}", user.getId());
        });

        // Always return same message — prevent user enumeration
        return RESET_REQUESTED_MSG;
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = tokenHashService.hash(rawToken);

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, INVALID_TOKEN_MSG));

        if (!resetToken.isValid()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, INVALID_TOKEN_MSG);
        }

        var user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, INVALID_TOKEN_MSG));

        user.setPasswordHash(passwordService.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        // Revoke all active refresh tokens — forces re-login on all devices
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("Password reset completed for user id={}", user.getId());
    }
}
