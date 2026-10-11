package org.example.backend.config;

import org.example.backend.service.CapabilityResult;
import org.example.backend.service.CapabilityStatus;
import org.example.backend.service.DefaultLabAvailabilityQuery;
import org.example.backend.service.DefaultLabCatalogQuery;
import org.example.backend.service.DefaultReservationAvailabilityEvaluator;
import org.example.backend.service.DefaultReservationCatalogQuery;
import org.example.backend.service.DefaultReservationCommandHandler;
import org.example.backend.service.DefaultReservationCreationService;
import org.example.backend.service.DefaultReservationReviewService;
import org.example.backend.service.DefaultScheduleOccupancyQuery;
import org.example.backend.service.DefaultTeachingScheduleQuery;
import org.example.backend.service.EnvironmentSnapshot;
import org.example.backend.service.EnvironmentSnapshotPort;
import org.example.backend.service.EnvironmentSnapshotQuery;
import org.example.backend.service.EnvironmentSnapshotView;
import org.example.backend.service.LabAvailabilityQuery;
import org.example.backend.service.LabCatalogQuery;
import org.example.backend.service.ReservationCatalogQuery;
import org.example.backend.service.ReservationCommandHandler;
import org.example.backend.service.ReservationLabService;
import org.example.backend.service.ReservationScheduleService;
import org.example.backend.service.ReservationSnapshot;
import org.example.backend.service.ReservationSnapshotPort;
import org.example.backend.service.ReservationSnapshotQuery;
import org.example.backend.service.ReservationSnapshotView;
import org.example.backend.service.ReservationAvailabilityEvaluator;
import org.example.backend.service.ReservationPersistenceService;
import org.example.backend.service.ScheduleOccupancyQuery;
import org.example.backend.service.ScheduleSnapshot;
import org.example.backend.service.ScheduleSnapshotPort;
import org.example.backend.service.ScheduleSnapshotQuery;
import org.example.backend.service.ScheduleSnapshotView;
import org.example.backend.service.LabRecordReadService;
import org.example.backend.service.TeachingScheduleQuery;
import org.example.backend.service.TeachingScheduleReadService;
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
    public LabCatalogQuery labCatalogQuery(
            LabRecordReadService records,
            org.example.backend.service.ManagerNamePort managerNames,
            org.example.backend.service.DeviceCountPort deviceCounts
    ) {
        return new DefaultLabCatalogQuery(records, managerNames, deviceCounts);
    }

    @Bean
    public LabAvailabilityQuery labAvailabilityQuery(LabRecordReadService records) {
        return new DefaultLabAvailabilityQuery(records);
    }

    @Bean
    public TeachingScheduleQuery teachingScheduleQuery(TeachingScheduleReadService readService) {
        return new DefaultTeachingScheduleQuery(readService);
    }

    @Bean
    public ScheduleOccupancyQuery scheduleOccupancyQuery(TeachingScheduleReadService readService) {
        return new DefaultScheduleOccupancyQuery(readService);
    }

    @Bean
    public ReservationCatalogQuery reservationCatalogQuery(
            ReservationPersistenceService reservations,
            ReservationLabService laboratories
    ) {
        return new DefaultReservationCatalogQuery(reservations, laboratories);
    }

    @Bean
    public ReservationAvailabilityEvaluator reservationAvailabilityEvaluator(
            ReservationPersistenceService reservations,
            ReservationLabService laboratories,
            ReservationScheduleService schedules
    ) {
        return new DefaultReservationAvailabilityEvaluator(reservations, laboratories, schedules);
    }

    @Bean
    public DefaultReservationCreationService reservationCreationService(
            ReservationPersistenceService reservations,
            ReservationCatalogQuery catalog,
            ReservationLabService laboratories,
            ReservationAvailabilityEvaluator availability
    ) {
        return new DefaultReservationCreationService(reservations, catalog, laboratories, availability);
    }

    @Bean
    public DefaultReservationReviewService reservationReviewService(
            ReservationPersistenceService reservations,
            ReservationCatalogQuery catalog,
            ReservationAvailabilityEvaluator availability
    ) {
        return new DefaultReservationReviewService(reservations, catalog, availability);
    }

    @Bean
    public ReservationCommandHandler reservationCommandHandler(
            DefaultReservationCreationService creation,
            DefaultReservationReviewService review
    ) {
        return new DefaultReservationCommandHandler(creation, review);
    }

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
