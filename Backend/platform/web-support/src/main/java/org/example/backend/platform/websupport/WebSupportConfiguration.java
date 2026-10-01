package org.example.backend.platform.websupport;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration(proxyBeanMethods = false)
public class WebSupportConfiguration {
    @Bean
    public CorrelationIdProvider correlationIdProvider() {
        return () -> UUID.randomUUID().toString();
    }
}
