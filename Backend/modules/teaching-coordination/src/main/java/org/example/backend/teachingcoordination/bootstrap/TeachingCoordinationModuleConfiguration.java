package org.example.backend.teachingcoordination.bootstrap;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.module.ModuleDescriptor;
import org.example.backend.kernel.module.ModuleId;
import org.example.backend.teachingcoordination.application.EnvironmentSnapshotPort;
import org.example.backend.teachingcoordination.application.ReservationSnapshotPort;
import org.example.backend.teachingcoordination.application.ScheduleSnapshotPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class TeachingCoordinationModuleConfiguration {
    @Bean
    public ModuleDescriptor teachingCoordinationModuleDescriptor() {
        return new ModuleDescriptor(new ModuleId("teaching-coordination"), "Teaching coordination", Set.of("coordination.read"));
    }

    @Bean
    @ConditionalOnMissingBean(ScheduleSnapshotPort.class)
    public ScheduleSnapshotPort unavailableScheduleSnapshotPort() {
        return teachingTaskId -> CapabilityResult.unavailable("teaching-schedule capability is unavailable");
    }

    @Bean
    @ConditionalOnMissingBean(ReservationSnapshotPort.class)
    public ReservationSnapshotPort unavailableReservationSnapshotPort() {
        return teachingTaskId -> CapabilityResult.unavailable("reservation capability is unavailable");
    }

    @Bean
    @ConditionalOnMissingBean(EnvironmentSnapshotPort.class)
    public EnvironmentSnapshotPort unavailableEnvironmentSnapshotPort() {
        return teachingTaskId -> CapabilityResult.unavailable("environment-readiness capability is unavailable");
    }
}
