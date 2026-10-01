package org.example.backend.platform.security;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SecurityPlatformConfigurationTest {
    @Test
    void defaultsToDenyWhenNoBusinessPolicyIsInstalled() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SecurityPlatformConfiguration.class)) {
            AuthorizationPolicy policy = context.getBean(AuthorizationPolicy.class);
            assertFalse(policy.isAllowed("anonymous", "reservation.approve"));
        }
    }
}
