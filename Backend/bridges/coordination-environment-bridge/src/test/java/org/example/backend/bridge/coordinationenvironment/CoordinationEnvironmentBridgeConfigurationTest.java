package org.example.backend.bridge.coordinationenvironment;

import org.example.backend.environmentreadiness.api.EnvironmentSnapshotQuery;
import org.example.backend.environmentreadiness.api.EnvironmentSnapshotView;
import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.teachingcoordination.api.EnvironmentSnapshotPort;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoordinationEnvironmentBridgeConfigurationTest {
    @Test
    void mapsEnvironmentViewIntoCoordinationSnapshot() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ProviderFixture.class, CoordinationEnvironmentBridgeConfiguration.class)) {
            var result = context.getBean(EnvironmentSnapshotPort.class).findByTeachingTaskId(7L);
            assertEquals(CapabilityStatus.AVAILABLE, result.status());
            assertEquals("AWAITING_CONFIRMATION", result.value().status());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ProviderFixture {
        @Bean
        EnvironmentSnapshotQuery environmentSnapshotQuery() {
            return id -> CapabilityResult.available(new EnvironmentSnapshotView(id, 23L, "AWAITING_CONFIRMATION"));
        }
    }
}
