package com.insightflow.integration.connector.kiotviet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Component
public class KiotVietWebhookVerifier {

    public boolean verify(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            log.warn("Webhook verification failed: null input");
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            boolean match = MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
            if (!match) {
                log.warn("Webhook HMAC mismatch — possible tampering");
            }
            return match;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
