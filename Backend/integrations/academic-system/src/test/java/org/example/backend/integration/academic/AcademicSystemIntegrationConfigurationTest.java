package org.example.backend.integration.academic;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AcademicSystemIntegrationConfigurationTest {
    @Test
    void startsDisabledWithoutCredentialsOrNetworkAccess() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AcademicSystemIntegrationConfiguration.class)) {
            assertFalse(context.getBean(AcademicSystemAvailability.class).enabled());
        }
    }
}
