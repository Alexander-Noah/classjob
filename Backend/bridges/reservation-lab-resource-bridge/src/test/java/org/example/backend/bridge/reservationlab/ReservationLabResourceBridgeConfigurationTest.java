package org.example.backend.bridge.reservationlab;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.labresource.api.LabAvailabilityQuery;
import org.example.backend.labresource.api.LabAvailabilityView;
import org.example.backend.reservation.application.LabAvailabilityPort;
import org.example.backend.reservation.domain.TimeRange;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservationLabResourceBridgeConfigurationTest {
    @Test
    void mapsProviderViewIntoConsumerOwnedSnapshot() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                ProviderFixture.class,
                ReservationLabResourceBridgeConfiguration.class
        )) {
            TimeRange range = new TimeRange(Instant.parse("2026-10-01T01:00:00Z"), Instant.parse("2026-10-01T03:00:00Z"));
            var result = context.getBean(LabAvailabilityPort.class).check(9L, 30, range);

            assertEquals(CapabilityStatus.AVAILABLE, result.status());
            assertEquals(9L, result.value().labId());
            assertEquals(60, result.value().capacity());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ProviderFixture {
        @Bean
        LabAvailabilityQuery labAvailabilityQuery() {
            return (labId, expectedCapacity, startsAt, endsAt) -> CapabilityResult.available(new LabAvailabilityView(labId, 60, true));
        }
    }
}
