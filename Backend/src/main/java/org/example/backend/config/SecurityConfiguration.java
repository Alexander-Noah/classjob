package org.example.backend.config;

import org.example.backend.security.AuthorizationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    @Bean
    public AuthorizationPolicy defaultDenyAuthorizationPolicy() {
        return (authority, action) -> false;
    }
}
