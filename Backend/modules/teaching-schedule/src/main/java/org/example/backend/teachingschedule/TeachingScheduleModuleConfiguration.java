package org.example.backend.teachingschedule;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.module.ModuleDescriptor;
import org.example.backend.kernel.module.ModuleId;
import org.example.backend.teachingschedule.api.ScheduleSnapshotQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class TeachingScheduleModuleConfiguration {
    @Bean
    public ModuleDescriptor teachingScheduleModuleDescriptor() {
        return new ModuleDescriptor(new ModuleId("teaching-schedule"), "Teaching schedule", Set.of("schedule.read", "schedule.import"));
    }

    @Bean
    @ConditionalOnMissingBean(ScheduleSnapshotQuery.class)
    public ScheduleSnapshotQuery unavailableScheduleSnapshotQuery() {
        return teachingTaskId -> CapabilityResult.unavailable("teaching-schedule storage is not migrated");
    }
}
