package org.example.backend.bridge.coordinationschedule;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.teachingcoordination.application.ScheduleSnapshotPort;
import org.example.backend.teachingschedule.api.ScheduleSnapshotQuery;
import org.example.backend.teachingschedule.api.ScheduleSnapshotView;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoordinationScheduleBridgeConfigurationTest {
    @Test
    void mapsScheduleViewIntoCoordinationSnapshot() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ProviderFixture.class, CoordinationScheduleBridgeConfiguration.class)) {
            var result = context.getBean(ScheduleSnapshotPort.class).findByTeachingTaskId(7L);
            assertEquals(CapabilityStatus.AVAILABLE, result.status());
            assertEquals("Data Lab", result.value().courseName());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ProviderFixture {
        @Bean
        ScheduleSnapshotQuery scheduleSnapshotQuery() {
            return id -> CapabilityResult.available(new ScheduleSnapshotView(id, "Data Lab", "Teacher", Instant.EPOCH, Instant.EPOCH.plusSeconds(3600)));
        }
    }
}
