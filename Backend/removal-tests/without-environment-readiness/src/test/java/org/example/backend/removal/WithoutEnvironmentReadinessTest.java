package org.example.backend.removal;

import org.example.backend.bridge.coordinationreservation.CoordinationReservationBridgeConfiguration;
import org.example.backend.bridge.coordinationschedule.CoordinationScheduleBridgeConfiguration;
import org.example.backend.bridge.reservationlab.ReservationLabResourceBridgeConfiguration;
import org.example.backend.identityaccess.IdentityAccessModuleConfiguration;
import org.example.backend.kernel.bootstrap.KernelConfiguration;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.kernel.module.ModuleCatalog;
import org.example.backend.kernel.module.ModuleId;
import org.example.backend.labresource.LabResourceModuleConfiguration;
import org.example.backend.platform.security.SecurityPlatformConfiguration;
import org.example.backend.platform.websupport.WebSupportConfiguration;
import org.example.backend.reservation.ReservationModuleConfiguration;
import org.example.backend.teachingcoordination.api.EnvironmentSnapshotPort;
import org.example.backend.teachingcoordination.TeachingCoordinationModuleConfiguration;
import org.example.backend.teachingschedule.TeachingScheduleModuleConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WithoutEnvironmentReadinessTest {

    @Test
    void startsWhenEnvironmentReadinessArtifactIsPhysicallyAbsent() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "org.example.backend.environmentreadiness.EnvironmentReadinessModuleConfiguration"));

        try (var context = new AnnotationConfigApplicationContext(WithoutEnvironmentReadinessConfiguration.class)) {
            ModuleCatalog catalog = context.getBean(ModuleCatalog.class);
            assertFalse(catalog.find(new ModuleId("environment-readiness")).isPresent());
            assertEquals(CapabilityStatus.UNAVAILABLE,
                    context.getBean(EnvironmentSnapshotPort.class).findByTeachingTaskId(1L).status());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            ReservationLabResourceBridgeConfiguration.class,
            CoordinationScheduleBridgeConfiguration.class,
            CoordinationReservationBridgeConfiguration.class,
            KernelConfiguration.class,
            SecurityPlatformConfiguration.class,
            WebSupportConfiguration.class,
            IdentityAccessModuleConfiguration.class,
            TeachingScheduleModuleConfiguration.class,
            LabResourceModuleConfiguration.class,
            ReservationModuleConfiguration.class,
            TeachingCoordinationModuleConfiguration.class
    })
    static class WithoutEnvironmentReadinessConfiguration {
    }
}
