package org.example.backend.host;

import org.example.backend.bridge.coordinationenvironment.CoordinationEnvironmentBridgeConfiguration;
import org.example.backend.bridge.coordinationreservation.CoordinationReservationBridgeConfiguration;
import org.example.backend.bridge.coordinationschedule.CoordinationScheduleBridgeConfiguration;
import org.example.backend.bridge.reservationlab.ReservationLabResourceBridgeConfiguration;
import org.example.backend.bridge.reservationschedule.ReservationTeachingScheduleBridgeConfiguration;
import org.example.backend.environmentreadiness.bootstrap.EnvironmentReadinessModuleConfiguration;
import org.example.backend.identityaccess.bootstrap.IdentityAccessModuleConfiguration;
import org.example.backend.integration.academic.AcademicSystemIntegrationConfiguration;
import org.example.backend.kernel.bootstrap.KernelConfiguration;
import org.example.backend.labresource.bootstrap.LabResourceRuntimeConfiguration;
import org.example.backend.platform.security.SecurityPlatformConfiguration;
import org.example.backend.platform.websupport.WebSupportConfiguration;
import org.example.backend.reservation.bootstrap.ReservationPersistenceRuntimeConfiguration;
import org.example.backend.teachingcoordination.bootstrap.TeachingCoordinationModuleConfiguration;
import org.example.backend.teachingschedule.bootstrap.TeachingScheduleRuntimeConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({
        ReservationLabResourceBridgeConfiguration.class,
        ReservationTeachingScheduleBridgeConfiguration.class,
        CoordinationScheduleBridgeConfiguration.class,
        CoordinationReservationBridgeConfiguration.class,
        CoordinationEnvironmentBridgeConfiguration.class,
        KernelConfiguration.class,
        SecurityPlatformConfiguration.class,
        WebSupportConfiguration.class,
        AcademicSystemIntegrationConfiguration.class,
        IdentityAccessModuleConfiguration.class,
        TeachingScheduleRuntimeConfiguration.class,
        LabResourceRuntimeConfiguration.class,
        ReservationPersistenceRuntimeConfiguration.class,
        EnvironmentReadinessModuleConfiguration.class,
        TeachingCoordinationModuleConfiguration.class
})
public class ModularHostConfiguration {
}
