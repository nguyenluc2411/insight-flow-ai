package com.insightflow.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    // Cost 12 ≈ 250ms on modern hardware — auth-agent spec requirement
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
