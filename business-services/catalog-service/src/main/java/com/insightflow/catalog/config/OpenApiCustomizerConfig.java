package com.insightflow.catalog.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiCustomizerConfig {

    private static final List<String> INTERNAL_HEADERS = List.of(
            "X-User-Id", "X-Tenant-Id", "X-Tenant-Slug",
            "X-Tenant-Plan", "X-User-Roles", "X-User-Permissions"
    );

    @Bean
    public OpenApiCustomizer hideInternalHeadersCustomizer() {
        return openApi -> openApi.getPaths().values()
                .forEach(pathItem -> pathItem.readOperations()
                        .forEach(op -> {
                            if (op.getParameters() == null) return;
                            op.getParameters().removeIf(p ->
                                    "header".equals(p.getIn()) &&
                                    INTERNAL_HEADERS.contains(p.getName())
                            );
                        }));
    }
}
