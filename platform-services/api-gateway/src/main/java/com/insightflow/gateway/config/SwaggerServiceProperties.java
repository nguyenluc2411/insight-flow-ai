package com.insightflow.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "app.swagger")
@Getter
@Setter
public class SwaggerServiceProperties {

    private List<ServiceEntry> services = new ArrayList<>();

    @Getter
    @Setter
    public static class ServiceEntry {
        private String alias;
        private String serviceId;
        /** Optional direct URL for services NOT registered with Eureka (e.g. Python FastAPI). */
        private String url;
    }

    /**
     * Prebuilt, immutable lookup map used by SwaggerDocsProxyController.
     * Only includes entries with a serviceId (Eureka-registered services).
     */
    public Map<String, String> aliasToServiceId() {
        return services.stream()
                .filter(e -> e.getServiceId() != null && !e.getServiceId().isBlank())
                .collect(Collectors.toUnmodifiableMap(
                        ServiceEntry::getAlias,
                        ServiceEntry::getServiceId));
    }

    /**
     * Direct URL lookup for services that bypass Eureka.
     * If an alias is here, the proxy uses a plain WebClient instead of lb.
     */
    public Map<String, String> aliasToDirectUrl() {
        Map<String, String> map = new HashMap<>();
        for (ServiceEntry e : services) {
            if (e.getUrl() != null && !e.getUrl().isBlank()) {
                map.put(e.getAlias(), e.getUrl());
            }
        }
        return Map.copyOf(map);
    }
}
