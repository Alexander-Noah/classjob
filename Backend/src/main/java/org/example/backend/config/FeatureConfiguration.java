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
import org.example.backend.service.LabAccessScope;
import org.example.backend.service.LabAvailabilityPort;
import org.example.backend.service.LabAvailabilityQuery;
import org.example.backend.service.LabAvailabilitySnapshot;
import org.example.backend.service.LabCatalogQuery;
import org.example.backend.service.LabView;
import org.example.backend.service.LaboratorySummary;
import org.example.backend.service.LaboratorySummaryPort;
import org.example.backend.service.ManagedLaboratoryIdsPort;
import org.example.backend.service.ReservationCatalogQuery;
import org.example.backend.service.ReservationCommandHandler;
import org.example.backend.service.ReservationSnapshot;
import org.example.backend.service.ReservationSnapshotPort;
import org.example.backend.service.ReservationSnapshotQuery;
import org.example.backend.service.ReservationSnapshotView;
import org.example.backend.service.ReservationAvailabilityEvaluator;
import org.example.backend.service.ReservationPersistenceService;
import org.example.backend.service.ScheduleConflictPort;
import org.example.backend.service.ScheduleConflictSnapshot;
import org.example.backend.service.ScheduleOccupancyQuery;
import org.example.backend.service.ScheduleOccupancyRequest;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wires the laboratory, timetable, and reservation features in the monolith.
 * Business collaboration stays explicit here so the application remains easy
 * to follow as one deployable Spring Boot project.
 */
@Configuration(proxyBeanMethods = false)
public class FeatureConfiguration {
    private static final ZoneId RESERVATION_TIME_ZONE = ZoneId.of("Asia/Shanghai");

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
            ManagedLaboratoryIdsPort managedLaboratoryIds,
            LaboratorySummaryPort laboratorySummaries
    ) {
        return new DefaultReservationCatalogQuery(reservations, managedLaboratoryIds, laboratorySummaries);
    }

    @Bean
    public ReservationAvailabilityEvaluator reservationAvailabilityEvaluator(
            ReservationPersistenceService reservations,
            LabAvailabilityPort labAvailability,
            ScheduleConflictPort scheduleConflicts
    ) {
        return new DefaultReservationAvailabilityEvaluator(reservations, labAvailability, scheduleConflicts);
    }

    @Bean
    public DefaultReservationCreationService reservationCreationService(
            ReservationPersistenceService reservations,
            ReservationCatalogQuery catalog,
            ManagedLaboratoryIdsPort managedLaboratoryIds,
            ReservationAvailabilityEvaluator availability
    ) {
        return new DefaultReservationCreationService(reservations, catalog, managedLaboratoryIds, availability);
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
    public LabAvailabilityPort labAvailabilityPort(LabAvailabilityQuery query) {
        return (labId, expectedCapacity, timeRange) -> {
            var result = query.check(labId, expectedCapacity, timeRange.startsAt(), timeRange.endsAt());
            if (result.status() == CapabilityStatus.UNAVAILABLE) {
                return CapabilityResult.unavailable(result.message());
            }
            var view = result.value();
            return CapabilityResult.available(new LabAvailabilitySnapshot(view.labId(), view.capacity(), view.available()));
        };
    }

    @Bean
    public ManagedLaboratoryIdsPort managedLaboratoryIdsPort(LabCatalogQuery query) {
        return managerUserId -> {
            List<LabView> labs = requireLabs(query.list(LabAccessScope.managedBy(managerUserId)));
            Set<Long> ids = new LinkedHashSet<>();
            for (LabView lab : labs) {
                Long id = requireLab(lab).id();
                if (id != null && id > 0 && !ids.add(id)) {
                    throw new IllegalStateException("lab catalog returned a duplicate laboratory id");
                }
            }
            return Set.copyOf(ids);
        };
    }

    @Bean
    public LaboratorySummaryPort laboratorySummaryPort(LabCatalogQuery query) {
        return laboratoryIds -> {
            if (laboratoryIds == null) {
                throw new IllegalArgumentException("laboratory ids are required");
            }
            if (laboratoryIds.isEmpty()) {
                return Map.of();
            }
            if (laboratoryIds.stream().anyMatch(id -> id == null || id <= 0)) {
                throw new IllegalArgumentException("laboratory ids must be positive");
            }
            Map<Long, LaboratorySummary> summaries = new LinkedHashMap<>();
            for (LabView lab : requireLabs(query.list(LabAccessScope.all()))) {
                LabView resolved = requireLab(lab);
                Long id = resolved.id();
                if (id == null || !laboratoryIds.contains(id)) {
                    continue;
                }
                if (summaries.putIfAbsent(id, new LaboratorySummary(resolved.labName(), resolved.labCode())) != null) {
                    throw new IllegalStateException("lab catalog returned a duplicate laboratory id");
                }
            }
            return Map.copyOf(summaries);
        };
    }

    @Bean
    public ScheduleConflictPort scheduleConflictPort(ScheduleOccupancyQuery schedules, LabCatalogQuery labs) {
        return (labId, date, range) -> checkScheduleConflict(schedules, labs, labId, date, range);
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

    private CapabilityResult<ScheduleConflictSnapshot> checkScheduleConflict(
            ScheduleOccupancyQuery schedules,
            LabCatalogQuery labs,
            Long labId,
            LocalDate date,
            org.example.backend.service.TimeRange range
    ) {
        if (labId == null || labId <= 0 || date == null || !valid(range)) {
            return unavailable("reservation request is malformed");
        }
        LocalDateTime start = LocalDateTime.ofInstant(range.startsAt(), RESERVATION_TIME_ZONE);
        LocalDateTime end = LocalDateTime.ofInstant(range.endsAt(), RESERVATION_TIME_ZONE);
        if (!start.toLocalDate().equals(date) || !end.toLocalDate().equals(date)) {
            return unavailable("reservation time must be within reservation date");
        }
        LabView lab;
        try {
            lab = labs.findById(labId, LabAccessScope.all());
        } catch (RuntimeException exception) {
            return unavailable("laboratory context is unavailable");
        }
        if (lab == null || !labId.equals(lab.id())) {
            return unavailable("laboratory context is unavailable");
        }
        Set<String> aliases = aliases(lab);
        if (aliases.isEmpty()) {
            return unavailable("laboratory identity is unavailable");
        }
        try {
            var result = schedules.check(new ScheduleOccupancyRequest(date, start.toLocalTime(), end.toLocalTime(), aliases));
            if (result == null || result.status() != CapabilityStatus.AVAILABLE || result.value() == null) {
                return unavailable(result == null || result.message() == null
                        ? "teaching schedule is unavailable" : result.message());
            }
            return CapabilityResult.available(new ScheduleConflictSnapshot(labId, date, range, result.value().occupied()));
        } catch (RuntimeException exception) {
            return unavailable("teaching schedule is unavailable");
        }
    }

    private Set<String> aliases(LabView lab) {
        Set<String> aliases = new LinkedHashSet<>();
        if (text(lab.labCode())) {
            aliases.add(lab.labCode().trim());
        }
        if (text(lab.labName())) {
            aliases.add(lab.labName().trim());
        }
        return Set.copyOf(aliases);
    }

    private boolean valid(org.example.backend.service.TimeRange range) {
        return range != null && range.startsAt() != null && range.endsAt() != null
                && range.startsAt().isBefore(range.endsAt());
    }

    private boolean text(String value) {
        return value != null && !value.isBlank();
    }

    private CapabilityResult<ScheduleConflictSnapshot> unavailable(String message) {
        return CapabilityResult.unavailable(message);
    }

    private List<LabView> requireLabs(List<LabView> labs) {
        if (labs == null) {
            throw new IllegalStateException("lab catalog is unavailable");
        }
        return labs;
    }

    private LabView requireLab(LabView lab) {
        if (lab == null) {
            throw new IllegalStateException("lab catalog returned malformed laboratory data");
        }
        return lab;
    }
}
