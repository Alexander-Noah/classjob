package org.example.backend.environmentreadiness;

import org.example.backend.environmentreadiness.api.EnvironmentSnapshotQuery;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.kernel.module.ModuleDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvironmentReadinessModuleConfigurationTest {
    @Test
    void startsInIsolationWithAnHonestUnavailableQuery() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(EnvironmentReadinessModuleConfiguration.class)) {
            assertEquals("environment-readiness", context.getBean(ModuleDescriptor.class).id().value());
            assertEquals(CapabilityStatus.UNAVAILABLE, context.getBean(EnvironmentSnapshotQuery.class).findByTeachingTaskId(1L).status());
        }
    }
}
