package org.example.backend.identityaccess.bootstrap;

import org.example.backend.kernel.module.ModuleDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdentityAccessModuleConfigurationTest {
    @Test
    void startsInIsolationAndPublishesItsDescriptor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(IdentityAccessModuleConfiguration.class)) {
            assertEquals("identity-access", context.getBean(ModuleDescriptor.class).id().value());
        }
    }
}
