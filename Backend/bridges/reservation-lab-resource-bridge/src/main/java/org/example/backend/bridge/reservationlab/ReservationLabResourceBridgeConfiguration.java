package org.example.backend.bridge.reservationlab;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.labresource.api.LabAvailabilityQuery;
import org.example.backend.reservation.application.LabAvailabilityPort;
import org.example.backend.reservation.application.LabAvailabilitySnapshot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ReservationLabResourceBridgeConfiguration {
    @Bean
    public LabAvailabilityPort bridgedLabAvailabilityPort(LabAvailabilityQuery query) {
        return (labId, expectedCapacity, timeRange) -> {
            var result = query.check(labId, expectedCapacity, timeRange.startsAt(), timeRange.endsAt());
            if (result.status() == CapabilityStatus.UNAVAILABLE) {
                return CapabilityResult.unavailable(result.message());
            }
            var view = result.value();
            return CapabilityResult.available(new LabAvailabilitySnapshot(view.labId(), view.capacity(), view.available()));
        };
    }
}
