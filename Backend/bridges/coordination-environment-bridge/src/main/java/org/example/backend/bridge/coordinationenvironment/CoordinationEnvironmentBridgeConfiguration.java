package org.example.backend.bridge.coordinationenvironment;

import org.example.backend.environmentreadiness.api.EnvironmentSnapshotQuery;
import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.teachingcoordination.application.EnvironmentSnapshot;
import org.example.backend.teachingcoordination.application.EnvironmentSnapshotPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CoordinationEnvironmentBridgeConfiguration {
    @Bean
    public EnvironmentSnapshotPort bridgedEnvironmentSnapshotPort(EnvironmentSnapshotQuery query) {
        return teachingTaskId -> {
            var result = query.findByTeachingTaskId(teachingTaskId);
            if (result.status() == CapabilityStatus.UNAVAILABLE) {
                return CapabilityResult.unavailable(result.message());
            }
            var view = result.value();
            return CapabilityResult.available(new EnvironmentSnapshot(view.teachingTaskId(), view.requestId(), view.status()));
        };
    }
}
