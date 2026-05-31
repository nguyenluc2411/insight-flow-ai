package com.insightflow.integration.connector.kiotviet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class KiotVietAuthClient {

    private final WebClient webClient;

    @Value("${app.integration.kiotviet.token-url:https://id.kiotviet.vn/connect/token}")
    private String tokenUrl;

    private final Map<String, TokenEntry> tokenCache = new ConcurrentHashMap<>();

    public KiotVietAuthClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String getAccessToken(String clientId, String clientSecret) {
        String cacheKey = clientId;
        TokenEntry cached = tokenCache.get(cacheKey);
        if (cached != null && cached.isValid()) {
            return cached.accessToken();
        }

        log.debug("Requesting new KiotViet access token for clientId=[REDACTED]");
        TokenResponse response = fetchToken(clientId, clientSecret);
        long expiresInSeconds = response.expiresIn() != null ? response.expiresIn() : 86400;
        Instant expiresAt = Instant.now().plusSeconds(expiresInSeconds - 60);
        tokenCache.put(cacheKey, new TokenEntry(response.accessToken(), expiresAt));
        log.info("KiotViet access token acquired, expires in {}s", expiresInSeconds - 60);
        return response.accessToken();
    }

    private TokenResponse fetchToken(String clientId, String clientSecret) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scopes", "PublicApi.Access");

        Map<?, ?> body = webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (body == null || !body.containsKey("access_token")) {
            throw new IllegalStateException("KiotViet token response missing access_token");
        }

        String accessToken = (String) body.get("access_token");
        Object expiresIn = body.get("expires_in");
        Long expiry = expiresIn instanceof Number n ? n.longValue() : null;
        return new TokenResponse(accessToken, expiry);
    }

    public void invalidate(String clientId) {
        tokenCache.remove(clientId);
        log.debug("KiotViet token cache invalidated for clientId=[REDACTED]");
    }

    private record TokenEntry(String accessToken, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }

    private record TokenResponse(String accessToken, Long expiresIn) {}
}
