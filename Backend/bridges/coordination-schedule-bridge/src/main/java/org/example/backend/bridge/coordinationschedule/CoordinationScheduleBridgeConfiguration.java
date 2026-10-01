package org.example.backend.bridge.coordinationschedule;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.teachingcoordination.application.ScheduleSnapshot;
import org.example.backend.teachingcoordination.application.ScheduleSnapshotPort;
import org.example.backend.teachingschedule.api.ScheduleSnapshotQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CoordinationScheduleBridgeConfiguration {
    @Bean
    public ScheduleSnapshotPort bridgedScheduleSnapshotPort(ScheduleSnapshotQuery query) {
        return teachingTaskId -> {
            var result = query.findByTeachingTaskId(teachingTaskId);
            if (result.status() == CapabilityStatus.UNAVAILABLE) {
                return CapabilityResult.unavailable(result.message());
            }
            var view = result.value();
            return CapabilityResult.available(new ScheduleSnapshot(
                    view.teachingTaskId(), view.courseName(), view.teacherName(), view.startsAt(), view.endsAt()
            ));
        };
    }
}
