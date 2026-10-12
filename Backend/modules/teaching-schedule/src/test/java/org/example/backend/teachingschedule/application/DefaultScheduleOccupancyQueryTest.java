package org.example.backend.teachingschedule.application;

import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.teachingschedule.api.ScheduleOccupancyRequest;
import org.example.backend.teachingschedule.application.ScheduleCalendarData;
import org.example.backend.teachingschedule.application.ScheduleOccupancyCriteria;
import org.example.backend.teachingschedule.application.ScheduleOccupancyRow;
import org.example.backend.teachingschedule.application.TeachingScheduleReadPort;
import org.example.backend.teachingschedule.api.ScheduleEntryView;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;
import org.example.backend.teachingschedule.application.ScheduleSummaryData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultScheduleOccupancyQueryTest {
    private static final LocalDate THURSDAY_IN_WEEK_SIX = LocalDate.of(2026, 10, 8);

    @Test
    void ownsCalendarWeekWeekdayAndCompleteCandidateRead() {
        RecordingReadPort readPort = new RecordingReadPort(
                new ScheduleCalendarData("2026-2027-1", "2026-08-31"),
                List.of(new ScheduleOccupancyRow("2-207M-鸿蒙应用开发实训室", 1, 2))
        );
        DefaultScheduleOccupancyQuery query = new DefaultScheduleOccupancyQuery(readPort);

        var result = query.check(request("09:00", "09:30", "2-207", "鸿蒙应用开发实训室"));

        assertEquals(CapabilityStatus.AVAILABLE, result.status());
        assertTrue(result.value().occupied());
        assertEquals(new ScheduleOccupancyCriteria("2026-2027-1", 6, "星期四"), readPort.criteria);
    }

    @Test
    void structuredCodeMatchDoesNotAcceptLongerRoomCode() {
        RecordingReadPort matching = port(List.of(new ScheduleOccupancyRow("2-207M-鸿蒙应用开发实训室", 1, 2)));
        assertTrue(new DefaultScheduleOccupancyQuery(matching).check(request("09:00", "09:30", "2-207")).value().occupied());

        RecordingReadPort falsePositive = port(List.of(new ScheduleOccupancyRow("2-2070M-其他实验室", 1, 2)));
        assertFalse(new DefaultScheduleOccupancyQuery(falsePositive).check(request("09:00", "09:30", "2-207")).value().occupied());
    }

    @Test
    void mapsEverySectionPairAndTreatsTouchingBoundariesAsFree() {
        List<LocalTime> starts = List.of(LocalTime.of(8, 30), LocalTime.of(10, 20), LocalTime.of(14, 0),
                LocalTime.of(15, 50), LocalTime.of(18, 40), LocalTime.of(20, 20));
        List<LocalTime> ends = List.of(LocalTime.of(10, 0), LocalTime.of(11, 50), LocalTime.of(15, 30),
                LocalTime.of(17, 20), LocalTime.of(20, 10), LocalTime.of(21, 50));

        for (int pair = 0; pair < starts.size(); pair++) {
            RecordingReadPort readPort = port(List.of(new ScheduleOccupancyRow("LAB-01", pair * 2 + 1, pair * 2 + 2)));
            var result = new DefaultScheduleOccupancyQuery(readPort).check(new ScheduleOccupancyRequest(
                    THURSDAY_IN_WEEK_SIX, starts.get(pair), ends.get(pair), Set.of("LAB-01")));
            assertTrue(result.value().occupied());
        }

        DefaultScheduleOccupancyQuery query = new DefaultScheduleOccupancyQuery(
                port(List.of(new ScheduleOccupancyRow("LAB-01", 1, 2))));
        assertFalse(query.check(request("10:00", "10:20", "LAB-01")).value().occupied());
        assertTrue(query.check(request("09:59", "10:01", "LAB-01")).value().occupied());
    }

    @Test
    void failsClosedForInvalidRequestsCalendarAndTimetableRows() {
        assertEquals(CapabilityStatus.UNAVAILABLE,
                new DefaultScheduleOccupancyQuery(new RecordingReadPort(null, List.of()))
                        .check(request("09:00", "09:30", "LAB-01")).status());
        assertEquals(CapabilityStatus.UNAVAILABLE,
                new DefaultScheduleOccupancyQuery(new RecordingReadPort(
                        new ScheduleCalendarData("2026-2027-1", "bad-date"), List.of()))
                        .check(request("09:00", "09:30", "LAB-01")).status());
        assertEquals(CapabilityStatus.UNAVAILABLE,
                new DefaultScheduleOccupancyQuery(port(List.of(new ScheduleOccupancyRow("LAB-01", 0, 2))))
                        .check(request("09:00", "09:30", "LAB-01")).status());
        assertEquals(CapabilityStatus.UNAVAILABLE,
                new DefaultScheduleOccupancyQuery(port(null))
                        .check(request("09:00", "09:30", "LAB-01")).status());
        assertEquals(CapabilityStatus.UNAVAILABLE,
                new DefaultScheduleOccupancyQuery(port(List.of()))
                        .check(new ScheduleOccupancyRequest(THURSDAY_IN_WEEK_SIX, LocalTime.NOON, LocalTime.NOON, Set.of("LAB-01"))).status());
    }

    private ScheduleOccupancyRequest request(String start, String end, String... aliases) {
        return new ScheduleOccupancyRequest(THURSDAY_IN_WEEK_SIX, LocalTime.parse(start), LocalTime.parse(end), Set.of(aliases));
    }

    private RecordingReadPort port(List<ScheduleOccupancyRow> rows) {
        return new RecordingReadPort(new ScheduleCalendarData("2026-2027-1", "2026-08-31"), rows);
    }

    private static final class RecordingReadPort implements TeachingScheduleReadPort {
        private final ScheduleCalendarData calendar;
        private final List<ScheduleOccupancyRow> rows;
        private ScheduleOccupancyCriteria criteria;

        private RecordingReadPort(ScheduleCalendarData calendar, List<ScheduleOccupancyRow> rows) {
            this.calendar = calendar;
            this.rows = rows;
        }

        @Override public ScheduleCalendarData findActiveCalendar() { return calendar; }
        @Override public List<ScheduleOccupancyRow> findOccupancyCandidates(ScheduleOccupancyCriteria value) { criteria = value; return rows; }
        @Override public List<ScheduleEntryView> search(ScheduleSearchCriteria criteria) { throw new UnsupportedOperationException(); }
        @Override public ScheduleSummaryData summary() { throw new UnsupportedOperationException(); }
        @Override public List<String> listSemesters() { throw new UnsupportedOperationException(); }
    }
}
