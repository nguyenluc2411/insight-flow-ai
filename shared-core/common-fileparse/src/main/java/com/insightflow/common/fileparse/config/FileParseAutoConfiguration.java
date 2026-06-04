package com.insightflow.common.fileparse.config;

import com.insightflow.common.fileparse.DynamicFileParser;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Registers {@link DynamicFileParser} as a bean for any service that depends on
 * common-fileparse. {@code @ConditionalOnMissingBean} lets a service override with
 * its own definition if needed.
 */
@AutoConfiguration
public class FileParseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DynamicFileParser dynamicFileParser() {
        return new DynamicFileParser();
    }
}
