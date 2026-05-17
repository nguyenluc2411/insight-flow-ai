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
 * Proxies GET /v3/api-docs/{alias} → http://{serviceId}/v3/api-docs.
 *
 * Swagger UI calls this endpoint for each service listed in the dropdown.
 * Alias → serviceId mapping comes from app.swagger.services in application.yml.
 *
 * Served by RequestMappingHandlerMapping (order 0), so it bypasses gateway routing
 * and JwtAuthenticationFilter entirely — no JWT required for API doc fetches.
 *
 * Exact paths registered by springdoc (/v3/api-docs, /v3/api-docs/swagger-config)
 * take priority over our pattern /{alias}, so there is no conflict.
 */
@RestController
@RequestMapping("/v3/api-docs")
@Slf4j
public class SwaggerDocsProxyController {

    private static final String OPENAPI_PATH = "/v3/api-docs";

    private final WebClient lbWebClient;
    private final Map<String, String> aliasToServiceId;

    public SwaggerDocsProxyController(
            @Qualifier("lbWebClient") WebClient lbWebClient,
            SwaggerServiceProperties swaggerProps) {
        this.lbWebClient = lbWebClient;
        this.aliasToServiceId = swaggerProps.aliasToServiceId();
    }

    /**
     * alias regex [a-z]{2,20}: only lowercase letters, 2–20 chars.
     * Excludes hyphens → cannot match "swagger-config" even as a fallback.
     * Unknown aliases return 404 rather than proxying arbitrary hosts.
     */
    @GetMapping("/{alias:[a-z]{2,20}}")
    public Mono<ResponseEntity<String>> proxyApiDocs(@PathVariable String alias) {
        String serviceId = aliasToServiceId.get(alias);
        if (serviceId == null) {
            log.debug("api-docs proxy: unknown alias '{}'", alias);
            return Mono.just(ResponseEntity.notFound().build());
        }

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
}
