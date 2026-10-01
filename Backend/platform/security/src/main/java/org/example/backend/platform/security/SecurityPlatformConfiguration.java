package org.example.backend.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SecurityPlatformConfiguration {
    @Bean
    public AuthorizationPolicy defaultDenyAuthorizationPolicy() {
        return (authority, action) -> false;
    }
}
