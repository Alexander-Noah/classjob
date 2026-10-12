package org.example.backend.labresource;

import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.kernel.module.ModuleDescriptor;
import org.example.backend.labresource.api.LabAvailabilityQuery;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LabResourceModuleConfigurationTest {
    @Test
    void startsInIsolationWithAnHonestUnavailableQuery() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(LabResourceModuleConfiguration.class)) {
            assertEquals("lab-resource", context.getBean(ModuleDescriptor.class).id().value());
            assertEquals(CapabilityStatus.UNAVAILABLE, context.getBean(LabAvailabilityQuery.class)
                    .check(1L, 30, Instant.parse("2026-10-01T01:00:00Z"), Instant.parse("2026-10-01T03:00:00Z")).status());
        }
    }
}
