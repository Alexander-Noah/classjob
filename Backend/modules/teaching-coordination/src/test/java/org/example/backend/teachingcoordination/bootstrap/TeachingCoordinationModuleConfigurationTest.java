package org.example.backend.teachingcoordination.bootstrap;

import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.kernel.module.ModuleDescriptor;
import org.example.backend.teachingcoordination.application.EnvironmentSnapshotPort;
import org.example.backend.teachingcoordination.application.ReservationSnapshotPort;
import org.example.backend.teachingcoordination.application.ScheduleSnapshotPort;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeachingCoordinationModuleConfigurationTest {
    @Test
    void startsInIsolationWithFallbacksForEveryOptionalCapability() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TeachingCoordinationModuleConfiguration.class)) {
            assertEquals("teaching-coordination", context.getBean(ModuleDescriptor.class).id().value());
            assertEquals(CapabilityStatus.UNAVAILABLE, context.getBean(ScheduleSnapshotPort.class).findByTeachingTaskId(1L).status());
            assertEquals(CapabilityStatus.UNAVAILABLE, context.getBean(ReservationSnapshotPort.class).findByTeachingTaskId(1L).status());
            assertEquals(CapabilityStatus.UNAVAILABLE, context.getBean(EnvironmentSnapshotPort.class).findByTeachingTaskId(1L).status());
        }
    }
}
