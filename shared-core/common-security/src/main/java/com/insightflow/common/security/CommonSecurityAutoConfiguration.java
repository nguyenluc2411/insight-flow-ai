package com.insightflow.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class CommonSecurityAutoConfiguration {

    /**
     * Registers {@link TenantContextFilter} at the highest precedence so that
     * all downstream filters and controllers can safely call
     * {@link TenantContextHolder#requireTenantId()}.
     *
     * <p>Declare your own {@link TenantContextFilter} bean to override.
     */
    @Bean
    @ConditionalOnMissingBean(TenantContextFilter.class)
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilter() {
        FilterRegistrationBean<TenantContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TenantContextFilter());
        registration.addUrlPatterns("/*");
        // Run just after the earliest filters (e.g. logging), before security
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("tenantContextFilter");
        return registration;
    }
}
