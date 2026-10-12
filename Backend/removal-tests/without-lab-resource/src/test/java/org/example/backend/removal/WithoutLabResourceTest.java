package org.example.backend.removal;

import org.example.backend.bridge.coordinationenvironment.CoordinationEnvironmentBridgeConfiguration;
import org.example.backend.bridge.coordinationreservation.CoordinationReservationBridgeConfiguration;
import org.example.backend.bridge.coordinationschedule.CoordinationScheduleBridgeConfiguration;
import org.example.backend.environmentreadiness.EnvironmentReadinessModuleConfiguration;
import org.example.backend.identityaccess.IdentityAccessModuleConfiguration;
import org.example.backend.kernel.bootstrap.KernelConfiguration;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.kernel.module.ModuleCatalog;
import org.example.backend.kernel.module.ModuleId;
import org.example.backend.platform.security.SecurityPlatformConfiguration;
import org.example.backend.platform.websupport.WebSupportConfiguration;
import org.example.backend.reservation.application.LabAvailabilityPort;
import org.example.backend.reservation.ReservationModuleConfiguration;
import org.example.backend.reservation.domain.TimeRange;
import org.example.backend.teachingcoordination.TeachingCoordinationModuleConfiguration;
import org.example.backend.teachingschedule.TeachingScheduleModuleConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WithoutLabResourceTest {
    @Test
    void startsWhenLabResourceArtifactAndItsBridgeArePhysicallyAbsent() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "org.example.backend.labresource.LabResourceModuleConfiguration"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "org.example.backend.bridge.reservationlab.ReservationLabResourceBridgeConfiguration"));

        try (var context = new AnnotationConfigApplicationContext(WithoutLabResourceConfiguration.class)) {
            ModuleCatalog catalog = context.getBean(ModuleCatalog.class);
            assertFalse(catalog.find(new ModuleId("lab-resource")).isPresent());
            TimeRange range = new TimeRange(
                    Instant.parse("2026-10-01T01:00:00Z"),
                    Instant.parse("2026-10-01T03:00:00Z")
            );
            assertEquals(CapabilityStatus.UNAVAILABLE,
                    context.getBean(LabAvailabilityPort.class).check(1L, 30, range).status());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            CoordinationScheduleBridgeConfiguration.class,
            CoordinationReservationBridgeConfiguration.class,
            CoordinationEnvironmentBridgeConfiguration.class,
            KernelConfiguration.class,
            SecurityPlatformConfiguration.class,
            WebSupportConfiguration.class,
            IdentityAccessModuleConfiguration.class,
            TeachingScheduleModuleConfiguration.class,
            ReservationModuleConfiguration.class,
            EnvironmentReadinessModuleConfiguration.class,
            TeachingCoordinationModuleConfiguration.class
    })
    static class WithoutLabResourceConfiguration {
    }
}
