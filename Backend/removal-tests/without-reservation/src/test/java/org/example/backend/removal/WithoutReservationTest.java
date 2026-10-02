package org.example.backend.removal;

import org.example.backend.bridge.coordinationenvironment.CoordinationEnvironmentBridgeConfiguration;
import org.example.backend.bridge.coordinationschedule.CoordinationScheduleBridgeConfiguration;
import org.example.backend.environmentreadiness.bootstrap.EnvironmentReadinessModuleConfiguration;
import org.example.backend.identityaccess.bootstrap.IdentityAccessModuleConfiguration;
import org.example.backend.kernel.bootstrap.KernelConfiguration;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.kernel.module.ModuleCatalog;
import org.example.backend.kernel.module.ModuleId;
import org.example.backend.labresource.bootstrap.LabResourceModuleConfiguration;
import org.example.backend.platform.security.SecurityPlatformConfiguration;
import org.example.backend.platform.websupport.WebSupportConfiguration;
import org.example.backend.teachingcoordination.application.ReservationSnapshotPort;
import org.example.backend.teachingcoordination.bootstrap.TeachingCoordinationModuleConfiguration;
import org.example.backend.teachingschedule.bootstrap.TeachingScheduleModuleConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WithoutReservationTest {

    @Test
    void startsWhenReservationArtifactIsPhysicallyAbsent() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "org.example.backend.reservation.bootstrap.ReservationModuleConfiguration"));

        try (var context = new AnnotationConfigApplicationContext(WithoutReservationConfiguration.class)) {
            ModuleCatalog catalog = context.getBean(ModuleCatalog.class);
            assertFalse(catalog.find(new ModuleId("reservation")).isPresent());
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
