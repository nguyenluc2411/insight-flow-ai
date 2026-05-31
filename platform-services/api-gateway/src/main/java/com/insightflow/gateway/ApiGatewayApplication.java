package com.insightflow.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import reactor.core.publisher.Hooks;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    static {
        // Bridge Reactor Context -> MDC automatically on every operator boundary.
        // Spring Boot 3.x ships micrometer-context-propagation on the classpath;
        // this single call activates the bridge so that values written to Reactor
        // Context (e.g. "correlationId" in CorrelationIdFilter) are mirrored into
        // MDC without any per-chain MDC.put()/remove() bookkeeping.
        Hooks.enableAutomaticContextPropagation();
    }

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
