package org.example.backend.removal;

import org.example.backend.bridge.coordinationenvironment.CoordinationEnvironmentBridgeConfiguration;
import org.example.backend.bridge.coordinationschedule.CoordinationScheduleBridgeConfiguration;
import org.example.backend.environmentreadiness.EnvironmentReadinessModuleConfiguration;
import org.example.backend.environmentreadiness.api.EnvironmentSnapshotQuery;
import org.example.backend.identityaccess.IdentityAccessModuleConfiguration;
import org.example.backend.kernel.bootstrap.KernelConfiguration;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.kernel.module.ModuleCatalog;
import org.example.backend.kernel.module.ModuleId;
import org.example.backend.labresource.api.LabAvailabilityQuery;
import org.example.backend.labresource.LabResourceModuleConfiguration;
import org.example.backend.platform.security.SecurityPlatformConfiguration;
import org.example.backend.platform.websupport.WebSupportConfiguration;
import org.example.backend.teachingcoordination.api.ReservationSnapshotPort;
import org.example.backend.teachingcoordination.TeachingCoordinationModuleConfiguration;
import org.example.backend.teachingschedule.api.ScheduleSnapshotQuery;
import org.example.backend.teachingschedule.TeachingScheduleModuleConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WithoutReservationTest {

    @Test
    void startsWhenReservationArtifactIsPhysicallyAbsent() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "org.example.backend.reservation.ReservationModuleConfiguration"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "org.example.backend.bridge.reservationlab.ReservationLabResourceBridgeConfiguration"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "org.example.backend.bridge.reservationschedule.ReservationTeachingScheduleBridgeConfiguration"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "org.example.backend.bridge.coordinationreservation.CoordinationReservationBridgeConfiguration"));

        try (var context = new AnnotationConfigApplicationContext(WithoutReservationConfiguration.class)) {
            ModuleCatalog catalog = context.getBean(ModuleCatalog.class);
            assertFalse(catalog.find(new ModuleId("reservation")).isPresent());
            assertEquals(5, catalog.list().size());
            assertTrue(catalog.find(new ModuleId("identity-access")).isPresent());
            assertTrue(catalog.find(new ModuleId("teaching-schedule")).isPresent());
            assertTrue(catalog.find(new ModuleId("lab-resource")).isPresent());
            assertTrue(catalog.find(new ModuleId("environment-readiness")).isPresent());
            assertTrue(catalog.find(new ModuleId("teaching-coordination")).isPresent());
            context.getBean(ScheduleSnapshotQuery.class);
            context.getBean(LabAvailabilityQuery.class);
            context.getBean(EnvironmentSnapshotQuery.class);
            assertEquals(CapabilityStatus.UNAVAILABLE,
                    context.getBean(ReservationSnapshotPort.class).findByTeachingTaskId(1L).status());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            CoordinationScheduleBridgeConfiguration.class,
            CoordinationEnvironmentBridgeConfiguration.class,
            KernelConfiguration.class,
            SecurityPlatformConfiguration.class,
            WebSupportConfiguration.class,
            IdentityAccessModuleConfiguration.class,
            TeachingScheduleModuleConfiguration.class,
            LabResourceModuleConfiguration.class,
            EnvironmentReadinessModuleConfiguration.class,
            TeachingCoordinationModuleConfiguration.class
    })
    static class WithoutReservationConfiguration {
    }
}
