package org.example.backend.removal;

import org.example.backend.bridge.coordinationenvironment.CoordinationEnvironmentBridgeConfiguration;
import org.example.backend.bridge.coordinationreservation.CoordinationReservationBridgeConfiguration;
import org.example.backend.bridge.coordinationschedule.CoordinationScheduleBridgeConfiguration;
import org.example.backend.bridge.reservationlab.ReservationLabResourceBridgeConfiguration;
import org.example.backend.environmentreadiness.bootstrap.EnvironmentReadinessModuleConfiguration;
import org.example.backend.identityaccess.IdentityAccessModuleConfiguration;
import org.example.backend.kernel.bootstrap.KernelConfiguration;
import org.example.backend.kernel.module.ModuleCatalog;
import org.example.backend.labresource.bootstrap.LabResourceModuleConfiguration;
import org.example.backend.platform.security.SecurityPlatformConfiguration;
import org.example.backend.platform.websupport.WebSupportConfiguration;
import org.example.backend.reservation.bootstrap.ReservationModuleConfiguration;
import org.example.backend.teachingcoordination.bootstrap.TeachingCoordinationModuleConfiguration;
import org.example.backend.teachingschedule.bootstrap.TeachingScheduleModuleConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WithoutAcademicSystemTest {

    @Test
    void startsWhenAcademicSystemArtifactIsPhysicallyAbsent() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "org.example.backend.integration.academic.AcademicSystemIntegrationConfiguration"));

        try (var context = new AnnotationConfigApplicationContext(WithoutAcademicSystemConfiguration.class)) {
            assertEquals(6, context.getBean(ModuleCatalog.class).list().size());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            ReservationLabResourceBridgeConfiguration.class,
            CoordinationScheduleBridgeConfiguration.class,
            CoordinationReservationBridgeConfiguration.class,
            CoordinationEnvironmentBridgeConfiguration.class,
            KernelConfiguration.class,
            SecurityPlatformConfiguration.class,
            WebSupportConfiguration.class,
            IdentityAccessModuleConfiguration.class,
            TeachingScheduleModuleConfiguration.class,
            LabResourceModuleConfiguration.class,
            ReservationModuleConfiguration.class,
            EnvironmentReadinessModuleConfiguration.class,
            TeachingCoordinationModuleConfiguration.class
    })
    static class WithoutAcademicSystemConfiguration {
    }
}
