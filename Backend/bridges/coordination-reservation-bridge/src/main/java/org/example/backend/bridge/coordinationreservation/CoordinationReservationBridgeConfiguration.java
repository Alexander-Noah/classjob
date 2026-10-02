package org.example.backend.bridge.coordinationreservation;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.reservation.api.ReservationSnapshotQuery;
import org.example.backend.teachingcoordination.application.ReservationSnapshot;
import org.example.backend.teachingcoordination.application.ReservationSnapshotPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CoordinationReservationBridgeConfiguration {
    @Bean
    public ReservationSnapshotPort bridgedReservationSnapshotPort(ReservationSnapshotQuery query) {
        return teachingTaskId -> {
            var result = query.findByTeachingTaskId(teachingTaskId);
            if (result.status() == CapabilityStatus.UNAVAILABLE) {
                return CapabilityResult.unavailable(result.message());
            }
            var view = result.value();
            return CapabilityResult.available(new ReservationSnapshot(view.teachingTaskId(), view.reservationId(), view.status()));
        };
    }
}
