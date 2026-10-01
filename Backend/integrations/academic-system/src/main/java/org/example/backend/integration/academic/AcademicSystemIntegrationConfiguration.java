package org.example.backend.integration.academic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AcademicSystemIntegrationConfiguration {
    @Bean
    public AcademicSystemAvailability academicSystemAvailability() {
        return new AcademicSystemAvailability(false);
    }
}
