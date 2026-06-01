package com.insightflow.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public UserContextFilter userContextFilter() {
        return new UserContextFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public CurrentUserArgumentResolver currentUserArgumentResolver() {
        return new CurrentUserArgumentResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityExceptionHandler securityExceptionHandler() {
        return new SecurityExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public PermissionAspect permissionAspect() {
        return new PermissionAspect();
    }
}
