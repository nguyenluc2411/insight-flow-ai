package com.insightflow.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Proxies GET /v3/api-docs/{alias} → downstream OpenAPI spec.
 *
 * Two flavours:
 *  - Eureka-registered services: http://{serviceId}/v3/api-docs via lbWebClient.
 *  - Direct-URL services (e.g. Python FastAPI): {url}/v3/api-docs via directWebClient.
 *
 * Alias → serviceId or alias → url mapping comes from app.swagger.services in YAML.
 *
 * Served by RequestMappingHandlerMapping (order 0), so it bypasses gateway routing
 * and JwtAuthenticationFilter entirely — no JWT required for API doc fetches.
 */
@RestController
@RequestMapping("/v3/api-docs")
@Slf4j
public class SwaggerDocsProxyController {

    private static final String OPENAPI_PATH = "/v3/api-docs";

    private final WebClient lbWebClient;
    private final WebClient directWebClient;
    private final Map<String, String> aliasToServiceId;
    private final Map<String, String> aliasToDirectUrl;

    public SwaggerDocsProxyController(
            @Qualifier("lbWebClient") WebClient lbWebClient,
            @Qualifier("directWebClient") WebClient directWebClient,
            SwaggerServiceProperties swaggerProps) {
        this.lbWebClient = lbWebClient;
        this.directWebClient = directWebClient;
        this.aliasToServiceId = swaggerProps.aliasToServiceId();
        this.aliasToDirectUrl = swaggerProps.aliasToDirectUrl();
    }

    /**
     * alias regex [a-z]{2,20}: only lowercase letters, 2–20 chars.
     */
    @GetMapping("/{alias:[a-z]{2,20}}")
    public Mono<ResponseEntity<String>> proxyApiDocs(@PathVariable String alias) {
        String directUrl = aliasToDirectUrl.get(alias);
        if (directUrl != null) {
            return proxyDirect(alias, directUrl);
        }

        String serviceId = aliasToServiceId.get(alias);
        if (serviceId == null) {
            log.debug("api-docs proxy: unknown alias '{}'", alias);
            return Mono.just(ResponseEntity.notFound().build());
        }
        return proxyLoadBalanced(alias, serviceId);
    }

    private Mono<ResponseEntity<String>> proxyLoadBalanced(String alias, String serviceId) {
        return lbWebClient.get()
                .uri("http://" + serviceId + OPENAPI_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .map(body -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(body))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.warn("api-docs proxy: HTTP {} from '{}' (alias='{}')",
                            ex.getStatusCode().value(), serviceId, alias);
                    return Mono.just(ResponseEntity.status(ex.getStatusCode())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(ex.getResponseBodyAsString()));
                })
                .onErrorResume(ex -> {
                    log.error("api-docs proxy: failed to reach '{}' (alias='{}') — {}",
                            serviceId, alias, ex.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body("{\"error\":\"service-unavailable\",\"service\":\"" + serviceId + "\"}"));
                });
    }

    private Mono<ResponseEntity<String>> proxyDirect(String alias, String directUrl) {
        String trimmed = directUrl.endsWith("/")
                ? directUrl.substring(0, directUrl.length() - 1)
                : directUrl;
        String target = trimmed + OPENAPI_PATH;
        return directWebClient.get()
                .uri(target)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .map(body -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(body))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.warn("api-docs proxy (direct): HTTP {} from '{}' (alias='{}')",
                            ex.getStatusCode().value(), target, alias);
                    return Mono.just(ResponseEntity.status(ex.getStatusCode())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(ex.getResponseBodyAsString()));
                })
                .onErrorResume(ex -> {
                    log.error("api-docs proxy (direct): failed to reach '{}' (alias='{}') — {}",
                            target, alias, ex.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body("{\"error\":\"service-unavailable\",\"target\":\"" + target + "\"}"));
                });
    }
}
