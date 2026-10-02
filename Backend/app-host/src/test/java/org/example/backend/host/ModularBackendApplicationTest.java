package org.example.backend.host;

import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.kernel.module.ModuleCatalog;
import org.example.backend.reservation.application.LabAvailabilityPort;
import org.example.backend.reservation.domain.TimeRange;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = ModularBackendApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ModularBackendApplicationTest {
    @Autowired
    private ModuleCatalog moduleCatalog;

    @Autowired
    private LabAvailabilityPort labAvailabilityPort;

    @Test
    void startsWithoutInfrastructureAndRegistersAllMvpModules() {
        Set<String> moduleIds = moduleCatalog.list().stream()
                .map(descriptor -> descriptor.id().value())
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "identity-access",
                "teaching-schedule",
                "lab-resource",
                "reservation",
                "environment-readiness",
                "teaching-coordination"
        ), moduleIds);
    }

    @Test
    void connectedSkeletonReportsUnavailableUntilPersistenceMigration() {
        TimeRange range = new TimeRange(Instant.parse("2026-10-01T01:00:00Z"), Instant.parse("2026-10-01T03:00:00Z"));
        assertEquals(CapabilityStatus.UNAVAILABLE, labAvailabilityPort.check(1L, 30, range).status());
    }
}
