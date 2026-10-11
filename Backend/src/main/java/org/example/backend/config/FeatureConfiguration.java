package org.example.backend.config;

import org.example.backend.service.CapabilityResult;
import org.example.backend.service.CapabilityStatus;
import org.example.backend.service.EnvironmentSnapshot;
import org.example.backend.service.EnvironmentSnapshotPort;
import org.example.backend.service.EnvironmentSnapshotQuery;
import org.example.backend.service.EnvironmentSnapshotView;
import org.example.backend.service.ReservationSnapshot;
import org.example.backend.service.ReservationSnapshotPort;
import org.example.backend.service.ReservationSnapshotQuery;
import org.example.backend.service.ReservationSnapshotView;
import org.example.backend.service.ScheduleSnapshot;
import org.example.backend.service.ScheduleSnapshotPort;
import org.example.backend.service.ScheduleSnapshotQuery;
import org.example.backend.service.ScheduleSnapshotView;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the laboratory, timetable, and reservation features in the monolith.
 * Business collaboration stays explicit here so the application remains easy
 * to follow as one deployable Spring Boot project.
 */
@Configuration(proxyBeanMethods = false)
public class FeatureConfiguration {
    @Bean
    @ConditionalOnMissingBean(EnvironmentSnapshotQuery.class)
    public EnvironmentSnapshotQuery unavailableEnvironmentSnapshotQuery() {
        return teachingTaskId -> CapabilityResult.unavailable("environment data is unavailable");
    }

    @Bean
    @ConditionalOnMissingBean(ReservationSnapshotQuery.class)
    public ReservationSnapshotQuery unavailableReservationSnapshotQuery() {
        return teachingTaskId -> CapabilityResult.unavailable("reservation data is unavailable");
    }

    @Bean
    @ConditionalOnMissingBean(ScheduleSnapshotQuery.class)
    public ScheduleSnapshotQuery unavailableScheduleSnapshotQuery() {
        return teachingTaskId -> CapabilityResult.unavailable("teaching schedule data is unavailable");
    }

    @Bean
    public EnvironmentSnapshotPort environmentSnapshotPort(EnvironmentSnapshotQuery query) {
        return teachingTaskId -> {
            CapabilityResult<EnvironmentSnapshotView> result = query.findByTeachingTaskId(teachingTaskId);
            if (result.status() == CapabilityStatus.UNAVAILABLE) {
                return CapabilityResult.unavailable(result.message());
            }
            EnvironmentSnapshotView view = result.value();
            return CapabilityResult.available(new EnvironmentSnapshot(view.teachingTaskId(), view.requestId(), view.status()));
        };
    }

    @Bean
    public ReservationSnapshotPort reservationSnapshotPort(ReservationSnapshotQuery query) {
        return teachingTaskId -> {
            CapabilityResult<ReservationSnapshotView> result = query.findByTeachingTaskId(teachingTaskId);
            if (result.status() == CapabilityStatus.UNAVAILABLE) {
                return CapabilityResult.unavailable(result.message());
            }
            ReservationSnapshotView view = result.value();
            return CapabilityResult.available(new ReservationSnapshot(view.teachingTaskId(), view.reservationId(), view.status()));
        };
    }

    @Bean
    public ScheduleSnapshotPort scheduleSnapshotPort(ScheduleSnapshotQuery query) {
        return teachingTaskId -> {
            CapabilityResult<ScheduleSnapshotView> result = query.findByTeachingTaskId(teachingTaskId);
            if (result.status() == CapabilityStatus.UNAVAILABLE) {
                return CapabilityResult.unavailable(result.message());
            }
            ScheduleSnapshotView view = result.value();
            return CapabilityResult.available(new ScheduleSnapshot(
                    view.teachingTaskId(), view.courseName(), view.teacherName(), view.startsAt(), view.endsAt()
            ));
        };
    }

}
