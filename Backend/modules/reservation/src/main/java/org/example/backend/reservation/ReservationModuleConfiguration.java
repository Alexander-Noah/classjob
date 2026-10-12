package org.example.backend.reservation;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.module.ModuleDescriptor;
import org.example.backend.kernel.module.ModuleId;
import org.example.backend.reservation.api.ReservationSnapshotQuery;
import org.example.backend.reservation.application.LabAvailabilityPort;
import org.example.backend.reservation.application.ScheduleConflictPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class ReservationModuleConfiguration {
    @Bean
    public ModuleDescriptor reservationModuleDescriptor() {
        return new ModuleDescriptor(new ModuleId("reservation"), "Reservation", Set.of("reservation.read", "reservation.manage"));
    }

    @Bean
    @ConditionalOnMissingBean(LabAvailabilityPort.class)
    public LabAvailabilityPort unavailableLabAvailabilityPort() {
        return (labId, expectedCapacity, timeRange) -> CapabilityResult.unavailable("lab-resource capability is unavailable");
    }

    @Bean
    @ConditionalOnMissingBean(ScheduleConflictPort.class)
    public ScheduleConflictPort unavailableScheduleConflictPort() {
        return (labId, reservationDate, timeRange) -> CapabilityResult.unavailable("schedule capability is unavailable");
    }

    @Bean
    @ConditionalOnMissingBean(ReservationSnapshotQuery.class)
    public ReservationSnapshotQuery unavailableReservationSnapshotQuery() {
        return teachingTaskId -> CapabilityResult.unavailable("reservation storage is not migrated");
    }
}
