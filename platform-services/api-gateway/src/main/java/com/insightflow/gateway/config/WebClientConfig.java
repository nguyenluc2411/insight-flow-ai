package com.insightflow.gateway.config;

import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * Immutable, load-balanced WebClient used by SwaggerDocsProxyController.
     * Resolves http://{serviceId}/... URIs via Eureka before sending.
     *
     * 2 MB codec limit — large OpenAPI specs with examples can exceed the default 256 KB.
     */
    @Bean("lbWebClient")
    public WebClient lbWebClient(ReactorLoadBalancerExchangeFilterFunction lbFunction) {
        return WebClient.builder()
                .filter(lbFunction)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }
}
