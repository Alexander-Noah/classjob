package org.example.backend.reservation.bootstrap;

import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.kernel.module.ModuleDescriptor;
import org.example.backend.reservation.api.ReservationSnapshotQuery;
import org.example.backend.reservation.application.LabAvailabilityPort;
import org.example.backend.reservation.domain.TimeRange;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservationModuleConfigurationTest {
    @Test
    void startsInIsolationAndFallsBackWhenLabModuleIsAbsent() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ReservationModuleConfiguration.class)) {
            assertEquals("reservation", context.getBean(ModuleDescriptor.class).id().value());
            TimeRange timeRange = new TimeRange(Instant.parse("2026-10-01T01:00:00Z"), Instant.parse("2026-10-01T03:00:00Z"));
            assertEquals(CapabilityStatus.UNAVAILABLE, context.getBean(LabAvailabilityPort.class).check(1L, 30, timeRange).status());
            assertEquals(CapabilityStatus.UNAVAILABLE, context.getBean(ReservationSnapshotQuery.class).findByTeachingTaskId(1L).status());
        }
    }
}
