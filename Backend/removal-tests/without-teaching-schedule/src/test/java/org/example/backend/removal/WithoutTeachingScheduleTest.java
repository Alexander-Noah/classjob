package org.example.backend.removal;

import org.example.backend.bridge.coordinationenvironment.CoordinationEnvironmentBridgeConfiguration;
import org.example.backend.bridge.coordinationreservation.CoordinationReservationBridgeConfiguration;
import org.example.backend.bridge.reservationlab.ReservationLabResourceBridgeConfiguration;
import org.example.backend.environmentreadiness.bootstrap.EnvironmentReadinessModuleConfiguration;
import org.example.backend.identityaccess.IdentityAccessModuleConfiguration;
import org.example.backend.kernel.bootstrap.KernelConfiguration;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.kernel.module.ModuleCatalog;
import org.example.backend.kernel.module.ModuleId;
import org.example.backend.labresource.bootstrap.LabResourceModuleConfiguration;
import org.example.backend.platform.security.SecurityPlatformConfiguration;
import org.example.backend.platform.websupport.WebSupportConfiguration;
import org.example.backend.reservation.bootstrap.ReservationModuleConfiguration;
import org.example.backend.teachingcoordination.application.ScheduleSnapshotPort;
import org.example.backend.teachingcoordination.bootstrap.TeachingCoordinationModuleConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WithoutTeachingScheduleTest {
    @Test
    void startsWhenTeachingScheduleArtifactAndItsBridgeArePhysicallyAbsent() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "org.example.backend.teachingschedule.bootstrap.TeachingScheduleModuleConfiguration"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "org.example.backend.bridge.coordinationschedule.CoordinationScheduleBridgeConfiguration"));

        try (var context = new AnnotationConfigApplicationContext(WithoutTeachingScheduleConfiguration.class)) {
            ModuleCatalog catalog = context.getBean(ModuleCatalog.class);
            assertFalse(catalog.find(new ModuleId("teaching-schedule")).isPresent());
            assertEquals(CapabilityStatus.UNAVAILABLE,
                    context.getBean(ScheduleSnapshotPort.class).findByTeachingTaskId(1L).status());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            ReservationLabResourceBridgeConfiguration.class,
            CoordinationReservationBridgeConfiguration.class,
            CoordinationEnvironmentBridgeConfiguration.class,
            KernelConfiguration.class,
            SecurityPlatformConfiguration.class,
            WebSupportConfiguration.class,
            IdentityAccessModuleConfiguration.class,
            LabResourceModuleConfiguration.class,
            ReservationModuleConfiguration.class,
            EnvironmentReadinessModuleConfiguration.class,
            TeachingCoordinationModuleConfiguration.class
    })
    static class WithoutTeachingScheduleConfiguration {
    }
}
