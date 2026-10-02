package org.example.backend.labresource.bootstrap;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.module.ModuleDescriptor;
import org.example.backend.kernel.module.ModuleId;
import org.example.backend.labresource.api.LabAvailabilityQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class LabResourceModuleConfiguration {
    @Bean
    public ModuleDescriptor labResourceModuleDescriptor() {
        return new ModuleDescriptor(new ModuleId("lab-resource"), "Lab resource", Set.of("lab.read", "lab.availability"));
    }

    @Bean
    @ConditionalOnMissingBean(LabAvailabilityQuery.class)
    public LabAvailabilityQuery unavailableLabAvailabilityQuery() {
        return (labId, expectedCapacity, startsAt, endsAt) -> CapabilityResult.unavailable("lab-resource storage is not migrated");
    }
}
