package org.example.backend.teachingschedule.bootstrap;

import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.kernel.module.ModuleDescriptor;
import org.example.backend.teachingschedule.api.ScheduleSnapshotQuery;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeachingScheduleModuleConfigurationTest {
    @Test
    void startsInIsolationWithAnHonestUnavailableQuery() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TeachingScheduleModuleConfiguration.class)) {
            assertEquals("teaching-schedule", context.getBean(ModuleDescriptor.class).id().value());
            assertEquals(CapabilityStatus.UNAVAILABLE, context.getBean(ScheduleSnapshotQuery.class).findByTeachingTaskId(1L).status());
        }
    }
}
