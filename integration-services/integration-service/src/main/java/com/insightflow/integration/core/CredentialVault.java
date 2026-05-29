package com.insightflow.integration.core;

import com.ulisesbocchio.jasyptspringboot.encryptor.DefaultLazyEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CredentialVault {

    private final StringEncryptor encryptor;

    public CredentialVault(StringEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("Cannot encrypt null or blank credential");
        }
        return encryptor.encrypt(plaintext);
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            throw new IllegalArgumentException("Cannot decrypt null or blank credential");
        }
        return encryptor.decrypt(encrypted);
    }
}
