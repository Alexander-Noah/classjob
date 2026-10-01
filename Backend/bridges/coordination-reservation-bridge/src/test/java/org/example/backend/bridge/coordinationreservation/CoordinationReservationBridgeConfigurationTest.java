package org.example.backend.bridge.coordinationreservation;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.reservation.api.ReservationSnapshotQuery;
import org.example.backend.reservation.api.ReservationSnapshotView;
import org.example.backend.teachingcoordination.application.ReservationSnapshotPort;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoordinationReservationBridgeConfigurationTest {
    @Test
    void mapsReservationViewIntoCoordinationSnapshot() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ProviderFixture.class, CoordinationReservationBridgeConfiguration.class)) {
            var result = context.getBean(ReservationSnapshotPort.class).findByTeachingTaskId(7L);
            assertEquals(CapabilityStatus.AVAILABLE, result.status());
            assertEquals("APPROVED", result.value().status());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ProviderFixture {
        @Bean
        ReservationSnapshotQuery reservationSnapshotQuery() {
            return id -> CapabilityResult.available(new ReservationSnapshotView(id, 19L, "APPROVED"));
        }
    }
}
