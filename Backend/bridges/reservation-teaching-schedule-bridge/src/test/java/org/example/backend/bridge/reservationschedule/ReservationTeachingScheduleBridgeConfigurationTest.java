package org.example.backend.bridge.reservationschedule;

import org.example.backend.kernel.capability.*;
import org.example.backend.labresource.api.*;
import org.example.backend.reservation.application.ScheduleConflictPort;
import org.example.backend.reservation.domain.TimeRange;
import org.example.backend.teachingschedule.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTeachingScheduleBridgeConfigurationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 8);

    @Test void delegatesOnlyLocalTimeAndLaboratoryAliasesToScheduleCapability() {
        var schedules = new CapturingQuery(CapabilityResult.available(new ScheduleOccupancyView(true)));
        try (var context = context(lab(9L, "2-207", "鸿蒙应用开发实训室"), schedules)) {
            TimeRange range = range(DATE, "09:00", "09:30");
            var result = port(context).check(9L, DATE, range);
            assertEquals(CapabilityStatus.AVAILABLE, result.status());
            assertTrue(result.value().hasConflict());
            assertEquals(new ScheduleOccupancyRequest(DATE, LocalTime.of(9, 0), LocalTime.of(9, 30),
                    java.util.Set.of("2-207", "鸿蒙应用开发实训室")), schedules.request);
            assertEquals(range, result.value().timeRange());
        }
    }

    @Test void rejectsMissingLabAndCrossDateRangeBeforeCallingSchedule() {
        var schedules = new CapturingQuery(CapabilityResult.available(new ScheduleOccupancyView(false)));
        try (var context = context(null, schedules)) {
            assertEquals(CapabilityStatus.UNAVAILABLE, port(context).check(9L, DATE, range(DATE, "09:00", "09:30")).status());
        }
        try (var context = context(lab(9L, "2-207", "实验室"), schedules)) {
            TimeRange crossDate = new TimeRange(DATE.atTime(23, 0).atZone(zone()).toInstant(),
                    DATE.plusDays(1).atTime(1, 0).atZone(zone()).toInstant());
            assertEquals(CapabilityStatus.UNAVAILABLE, port(context).check(9L, DATE, crossDate).status());
        }
        assertEquals(0, schedules.calls);
    }

    @Test void failsClosedWhenScheduleCapabilityIsUnavailableOrThrows() {
        for (var schedules : List.of(new CapturingQuery(CapabilityResult.unavailable("down")),
                new CapturingQuery(null), CapturingQuery.throwing())) {
            try (var context = context(lab(9L, "2-207", "实验室"), schedules)) {
                assertEquals(CapabilityStatus.UNAVAILABLE,
                        port(context).check(9L, DATE, range(DATE, "09:00", "09:30")).status());
            }
        }
    }

    private static AnnotationConfigApplicationContext context(LabView lab, ScheduleOccupancyQuery schedules) {
        var context = new AnnotationConfigApplicationContext();
        context.registerBean(LabCatalogQuery.class, () -> new LabCatalogQuery() {
            public List<LabView> list(LabAccessScope scope) { throw new UnsupportedOperationException(); }
            public LabView findById(long id, LabAccessScope scope) { return lab; }
        });
        context.registerBean(ScheduleOccupancyQuery.class, () -> schedules);
        context.register(ReservationTeachingScheduleBridgeConfiguration.class);
        context.refresh();
        return context;
    }
    private static ScheduleConflictPort port(AnnotationConfigApplicationContext context) { return context.getBean(ScheduleConflictPort.class); }
    private static LabView lab(Long id, String code, String name) { return new LabView(id, code, name, null, null, null, null, null, null, null, null, 0, null, null, null); }
    private static TimeRange range(LocalDate date, String start, String end) { return new TimeRange(date.atTime(LocalTime.parse(start)).atZone(zone()).toInstant(), date.atTime(LocalTime.parse(end)).atZone(zone()).toInstant()); }
    private static ZoneId zone() { return ZoneId.of("Asia/Shanghai"); }

    private static final class CapturingQuery implements ScheduleOccupancyQuery {
        private final CapabilityResult<ScheduleOccupancyView> result; private final boolean throwing;
        private int calls; private ScheduleOccupancyRequest request;
        CapturingQuery(CapabilityResult<ScheduleOccupancyView> result) { this(result, false); }
        CapturingQuery(CapabilityResult<ScheduleOccupancyView> result, boolean throwing) { this.result = result; this.throwing = throwing; }
        static CapturingQuery throwing() { return new CapturingQuery(null, true); }
        public CapabilityResult<ScheduleOccupancyView> check(ScheduleOccupancyRequest request) {
            calls++; this.request = request; if (throwing) throw new IllegalStateException("down"); return result;
        }
    }
}
