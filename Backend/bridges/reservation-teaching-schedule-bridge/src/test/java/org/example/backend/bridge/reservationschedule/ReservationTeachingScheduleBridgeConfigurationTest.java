package org.example.backend.bridge.reservationschedule;

import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.labresource.api.LabAccessScope;
import org.example.backend.labresource.api.LabCatalogQuery;
import org.example.backend.labresource.api.LabView;
import org.example.backend.reservation.application.ScheduleConflictPort;
import org.example.backend.reservation.domain.TimeRange;
import org.example.backend.teachingschedule.api.ScheduleEntryView;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;
import org.example.backend.teachingschedule.api.ScheduleSummaryView;
import org.example.backend.teachingschedule.api.TeachingScheduleQuery;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationTeachingScheduleBridgeConfigurationTest {
    private static final LocalDate DEFAULT_START = LocalDate.of(2026, 3, 2);
    private static final LocalDate MONDAY_IN_WEEK_FIVE = LocalDate.of(2026, 3, 30);

    @Test
    void resolvesDefaultSemesterWeekFiltersChineseWeekdayAndEchoesTheRequest() {
        CapturingScheduleQuery schedules = new CapturingScheduleQuery(List.of(entry("星期一", "LAB-01", 1, 2)));
        TimeRange request = range("09:00", "09:30");
        try (AnnotationConfigApplicationContext context = context(lab(9L, "LAB-01", "数据实验室"), schedules, Map.of())) {
            var result = port(context).check(9L, MONDAY_IN_WEEK_FIVE, request);

            assertEquals(CapabilityStatus.AVAILABLE, result.status());
            assertTrue(result.value().hasConflict());
            assertEquals(9L, result.value().labId());
            assertEquals(MONDAY_IN_WEEK_FIVE, result.value().reservationDate());
            assertEquals(request, result.value().timeRange());
            assertEquals(new ScheduleSearchCriteria(null, null, null, null, null, null, 5, 5000), schedules.criteria());
        }
    }

    @Test
    void usesConfiguredSemesterStartDate() {
        CapturingScheduleQuery schedules = new CapturingScheduleQuery(List.of());
        try (AnnotationConfigApplicationContext context = context(lab(9L, "LAB-01", "数据实验室"), schedules,
                Map.of("reservation.schedule.semester-start-date", "2026-09-07"))) {
            var result = port(context).check(9L, LocalDate.of(2026, 9, 14), range("09:00", "09:30"));

            assertEquals(CapabilityStatus.AVAILABLE, result.status());
            assertEquals(2, schedules.criteria().week());
        }
    }

    @Test
    void rejectsDatesBeforeSemesterStartAndInvalidConfiguredDates() {
        CapturingScheduleQuery schedules = new CapturingScheduleQuery(List.of());
        try (AnnotationConfigApplicationContext beforeStart = context(lab(9L, "LAB-01", "数据实验室"), schedules, Map.of())) {
            assertEquals(CapabilityStatus.UNAVAILABLE, port(beforeStart).check(9L, DEFAULT_START.minusDays(1), range("09:00", "09:30")).status());
            assertEquals(0, schedules.calls());
        }

        try (AnnotationConfigApplicationContext invalidConfiguredStart = context(lab(9L, "LAB-01", "数据实验室"), schedules,
                Map.of("reservation.schedule.semester-start-date", "not-a-date"))) {
            assertEquals(CapabilityStatus.UNAVAILABLE, port(invalidConfiguredStart).check(9L, MONDAY_IN_WEEK_FIVE, range("09:00", "09:30")).status());
            assertEquals(0, schedules.calls());
        }
    }

    @Test
    void rejectsMissingOrMismatchedLaboratories() {
        CapturingScheduleQuery schedules = new CapturingScheduleQuery(List.of());
        try (AnnotationConfigApplicationContext missing = context(null, schedules, Map.of())) {
            assertEquals(CapabilityStatus.UNAVAILABLE, port(missing).check(9L, MONDAY_IN_WEEK_FIVE, range("09:00", "09:30")).status());
        }
        try (AnnotationConfigApplicationContext mismatched = context(lab(8L, "LAB-01", "数据实验室"), schedules, Map.of())) {
            assertEquals(CapabilityStatus.UNAVAILABLE, port(mismatched).check(9L, MONDAY_IN_WEEK_FIVE, range("09:00", "09:30")).status());
        }
    }

    @Test
    void matchesExactLabCodesAndNamesWithoutSubstringFalsePositives() {
        CapturingScheduleQuery schedules = new CapturingScheduleQuery(List.of(
                entry("星期一", "LAB-010", 1, 2),
                entry("星期一", "数据实验室", 1, 2)
        ));
        try (AnnotationConfigApplicationContext context = context(lab(9L, "LAB-01", "数据实验室"), schedules, Map.of())) {
            assertTrue(port(context).check(9L, MONDAY_IN_WEEK_FIVE, range("09:00", "09:30")).value().hasConflict());
        }

        CapturingScheduleQuery falsePositiveOnly = new CapturingScheduleQuery(List.of(entry("星期一", "LAB-010", 1, 2)));
        try (AnnotationConfigApplicationContext context = context(lab(9L, "LAB-01", "其他实验室"), falsePositiveOnly, Map.of())) {
            assertFalse(port(context).check(9L, MONDAY_IN_WEEK_FIVE, range("09:00", "09:30")).value().hasConflict());
        }
    }

    @Test
    void mapsEverySectionPairToItsTimetableBoundary() {
        List<LocalTime> starts = List.of(LocalTime.of(8, 30), LocalTime.of(10, 20), LocalTime.of(14, 0), LocalTime.of(15, 50), LocalTime.of(18, 40), LocalTime.of(20, 20));
        List<LocalTime> ends = List.of(LocalTime.of(10, 0), LocalTime.of(11, 50), LocalTime.of(15, 30), LocalTime.of(17, 20), LocalTime.of(20, 10), LocalTime.of(21, 50));
        for (int pair = 0; pair < starts.size(); pair++) {
            CapturingScheduleQuery schedules = new CapturingScheduleQuery(List.of(entry("星期一", "LAB-01", pair * 2 + 1, pair * 2 + 2)));
            try (AnnotationConfigApplicationContext context = context(lab(9L, "LAB-01", "数据实验室"), schedules, Map.of())) {
                assertTrue(port(context).check(9L, MONDAY_IN_WEEK_FIVE, timeRange(starts.get(pair), ends.get(pair))).value().hasConflict());
            }
        }
    }

    @Test
    void treatsTouchingTimetableBoundariesAsNonConflicts() {
        CapturingScheduleQuery schedules = new CapturingScheduleQuery(List.of(entry("星期一", "LAB-01", 1, 2)));
        try (AnnotationConfigApplicationContext context = context(lab(9L, "LAB-01", "数据实验室"), schedules, Map.of())) {
            assertFalse(port(context).check(9L, MONDAY_IN_WEEK_FIVE, range("10:00", "10:20")).value().hasConflict());
            assertTrue(port(context).check(9L, MONDAY_IN_WEEK_FIVE, range("09:59", "10:01")).value().hasConflict());
        }
    }

    @Test
    void failsClosedForMalformedScheduleDataAndScheduleQueryFailures() {
        List<CapturingScheduleQuery> invalidQueries = List.of(
                new CapturingScheduleQuery(null),
                new CapturingScheduleQuery(List.of(entry("星期一", "LAB-01", 0, 2))),
                new CapturingScheduleQuery(List.of(entry("星期一", "LAB-01", 1, 13))),
                new CapturingScheduleQuery(java.util.Collections.singletonList(null)),
                CapturingScheduleQuery.throwing()
        );
        for (CapturingScheduleQuery schedules : invalidQueries) {
            try (AnnotationConfigApplicationContext context = context(lab(9L, "LAB-01", "数据实验室"), schedules, Map.of())) {
                assertEquals(CapabilityStatus.UNAVAILABLE, port(context).check(9L, MONDAY_IN_WEEK_FIVE, range("09:00", "09:30")).status());
            }
        }
    }

    private static AnnotationConfigApplicationContext context(LabView lab, TeachingScheduleQuery schedules, Map<String, Object> properties) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
        context.registerBean(LabCatalogQuery.class, () -> new LabCatalogQuery() {
            @Override
            public List<LabView> list(LabAccessScope scope) {
                throw new UnsupportedOperationException();
            }

            @Override
            public LabView findById(long id, LabAccessScope scope) {
                assertEquals(LabAccessScope.all(), scope);
                return lab;
            }
        });
        context.registerBean(TeachingScheduleQuery.class, () -> schedules);
        context.register(ReservationTeachingScheduleBridgeConfiguration.class);
        context.refresh();
        return context;
    }

    private static ScheduleConflictPort port(AnnotationConfigApplicationContext context) {
        return context.getBean(ScheduleConflictPort.class);
    }

    private static LabView lab(Long id, String code, String name) {
        return new LabView(id, code, name, null, null, null, null, null, null, null, null, 0, null, null, null);
    }

    private static ScheduleEntryView entry(String weekday, String classroom, int startSection, int endSection) {
        return new ScheduleEntryView(1L, null, null, null, weekday, null, null, startSection, endSection, null, null, null, null, null, classroom, null);
    }

    private static TimeRange range(String start, String end) {
        return timeRange(LocalTime.parse(start), LocalTime.parse(end));
    }

    private static TimeRange timeRange(LocalTime start, LocalTime end) {
        return new TimeRange(
                LocalDate.of(2026, 3, 30).atTime(start).toInstant(java.time.ZoneOffset.UTC),
                LocalDate.of(2026, 3, 30).atTime(end).toInstant(java.time.ZoneOffset.UTC)
        );
    }

    private static final class CapturingScheduleQuery implements TeachingScheduleQuery {
        private final List<ScheduleEntryView> entries;
        private final boolean throwsOnSearch;
        private final AtomicInteger calls = new AtomicInteger();
        private final List<ScheduleSearchCriteria> searched = new ArrayList<>();

        private CapturingScheduleQuery(List<ScheduleEntryView> entries) {
            this(entries, false);
        }

        private CapturingScheduleQuery(List<ScheduleEntryView> entries, boolean throwsOnSearch) {
            this.entries = entries;
            this.throwsOnSearch = throwsOnSearch;
        }

        static CapturingScheduleQuery throwing() {
            return new CapturingScheduleQuery(List.of(), true);
        }

        @Override
        public List<ScheduleEntryView> search(ScheduleSearchCriteria criteria) {
            calls.incrementAndGet();
            searched.add(criteria);
            if (throwsOnSearch) {
                throw new IllegalStateException("schedule down");
            }
            return entries;
        }

        @Override
        public ScheduleSummaryView summary() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> listSemesters() {
            throw new UnsupportedOperationException();
        }

        int calls() {
            return calls.get();
        }

        ScheduleSearchCriteria criteria() {
            return searched.get(0);
        }
    }
}
