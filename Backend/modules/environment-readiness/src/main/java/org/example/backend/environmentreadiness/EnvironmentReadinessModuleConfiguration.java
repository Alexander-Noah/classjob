package org.example.backend.environmentreadiness;

import org.example.backend.environmentreadiness.api.EnvironmentSnapshotQuery;
import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.module.ModuleDescriptor;
import org.example.backend.kernel.module.ModuleId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class EnvironmentReadinessModuleConfiguration {
    @Bean
    public ModuleDescriptor environmentReadinessModuleDescriptor() {
        return new ModuleDescriptor(new ModuleId("environment-readiness"), "Environment readiness", Set.of("environment.read", "environment.manage"));
    }

    @Bean
    @ConditionalOnMissingBean(EnvironmentSnapshotQuery.class)
    public EnvironmentSnapshotQuery unavailableEnvironmentSnapshotQuery() {
        return teachingTaskId -> CapabilityResult.unavailable("environment-readiness storage is not migrated");
    }
}
