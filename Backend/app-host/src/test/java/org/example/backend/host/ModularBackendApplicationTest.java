package org.example.backend.host;

import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.kernel.bootstrap.KernelConfiguration;
import org.example.backend.kernel.module.ModuleCatalog;
import org.example.backend.labresource.api.LabCatalogQuery;
import org.example.backend.labresource.api.LabView;
import org.example.backend.labresource.bootstrap.LabResourceModuleConfiguration;
import org.example.backend.reservation.application.ScheduleConflictPort;
import org.example.backend.reservation.application.LabAvailabilityPort;
import org.example.backend.reservation.bootstrap.ReservationModuleConfiguration;
import org.example.backend.reservation.domain.TimeRange;
import org.example.backend.teachingschedule.api.ScheduleOccupancyQuery;
import org.example.backend.teachingschedule.api.ScheduleOccupancyView;
import org.example.backend.teachingschedule.bootstrap.TeachingScheduleModuleConfiguration;
import org.example.backend.identityaccess.bootstrap.IdentityAccessModuleConfiguration;
import org.example.backend.environmentreadiness.bootstrap.EnvironmentReadinessModuleConfiguration;
import org.example.backend.teachingcoordination.bootstrap.TeachingCoordinationModuleConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModularBackendApplicationTest {
    @Test
    void startsInfrastructureFreeAndRegistersAllMvpModules() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(ModularBackendApplication.class)
                .web(WebApplicationType.NONE)
                .properties("spring.main.web-application-type=none", "server.port=-1")
                .run()) {
            Set<String> moduleIds = context.getBean(ModuleCatalog.class).list().stream()
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
    }

    @Test
    void startsWithoutInfrastructureAndRegistersAllMvpModules() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                KernelConfiguration.class,
                IdentityAccessModuleConfiguration.class,
                TeachingScheduleModuleConfiguration.class,
                LabResourceModuleConfiguration.class,
                ReservationModuleConfiguration.class,
                EnvironmentReadinessModuleConfiguration.class,
                TeachingCoordinationModuleConfiguration.class
        )) {
            Set<String> moduleIds = context.getBean(ModuleCatalog.class).list().stream()
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
    }

    @Test
    void connectedSkeletonReportsUnavailableUntilPersistenceMigration() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                LabResourceModuleConfiguration.class,
                org.example.backend.bridge.reservationlab.ReservationLabResourceBridgeConfiguration.class,
                ReservationModuleConfiguration.class
        )) {
            TimeRange range = new TimeRange(Instant.parse("2026-10-01T01:00:00Z"), Instant.parse("2026-10-01T03:00:00Z"));
            assertEquals(CapabilityStatus.UNAVAILABLE, context.getBean(LabAvailabilityPort.class).check(1L, 30, range).status());
        }
    }

    @Test
    void keepsReservationBridgesAndLeavesPersistenceRuntimeUnimported() {
        Import imported = ModularHostConfiguration.class.getAnnotation(Import.class);
        Set<String> importedTypes = Arrays.stream(imported.value()).map(Class::getName).collect(Collectors.toSet());

        assertTrue(importedTypes.contains("org.example.backend.bridge.reservationlab.ReservationLabResourceBridgeConfiguration"));
        assertTrue(importedTypes.contains("org.example.backend.bridge.reservationschedule.ReservationTeachingScheduleBridgeConfiguration"));
        assertFalse(importedTypes.contains("org.example.backend.reservation.bootstrap.ReservationPersistenceRuntimeConfiguration"));
    }

    @Test
    void realScheduleBridgeWinsOverTheFailClosedFallbackWhenItsModuleQueriesAreAvailable() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                ScheduleBridgeProviders.class,
                org.example.backend.bridge.reservationschedule.ReservationTeachingScheduleBridgeConfiguration.class,
                ReservationModuleConfiguration.class
        )) {
            TimeRange range = new TimeRange(Instant.parse("2026-03-30T00:30:00Z"), Instant.parse("2026-03-30T01:00:00Z"));
            var result = context.getBean(ScheduleConflictPort.class).check(9L, java.time.LocalDate.of(2026, 3, 30), range);

            assertEquals(CapabilityStatus.AVAILABLE, result.status());
            assertTrue(result.value().hasConflict());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ScheduleBridgeProviders {
        @Bean
        LabCatalogQuery labCatalogQuery() {
            return new LabCatalogQuery() {
                @Override
                public java.util.List<LabView> list(org.example.backend.labresource.api.LabAccessScope scope) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public LabView findById(long id, org.example.backend.labresource.api.LabAccessScope scope) {
                    return new LabView(id, "LAB-01", "数据实验室", null, null, null, null, null, null, null, null, 0, null, null, null);
                }
            };
        }

        @Bean
        ScheduleOccupancyQuery scheduleOccupancyQuery() {
            return request -> org.example.backend.kernel.capability.CapabilityResult.available(
                    new ScheduleOccupancyView(true));
        }
    }
}
