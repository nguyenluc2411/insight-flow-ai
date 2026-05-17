package com.insightflow.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
    }

    /**
     * Prebuilt, immutable lookup map used by SwaggerDocsProxyController.
     * Called once at construction time — YAML config is static.
     */
    public Map<String, String> aliasToServiceId() {
        return services.stream()
                .collect(Collectors.toUnmodifiableMap(
                        ServiceEntry::getAlias,
                        ServiceEntry::getServiceId));
    }
}
