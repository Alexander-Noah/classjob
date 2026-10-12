package org.example.backend.teachingschedule.application;

import org.example.backend.teachingschedule.api.ScheduleEntryView;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;
import org.example.backend.teachingschedule.api.ScheduleSummaryView;
import org.example.backend.teachingschedule.application.ScheduleSummaryData;
import org.example.backend.teachingschedule.application.TeachingScheduleReadPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultTeachingScheduleQueryTest {
    private final FakeTeachingScheduleReadPort mapper = new FakeTeachingScheduleReadPort();
    private final DefaultTeachingScheduleQuery query = new DefaultTeachingScheduleQuery(mapper);

    @Test
    void normalizesSearchAtTheUseCaseEntry() {
        query.search(new ScheduleSearchCriteria(
                " 2026-Spring ", " ", null, " Lab 201 ", " Networks ", " net ", 3, 9000
        ));

        assertEquals("2026-Spring", mapper.lastCriteria().semester());
        assertNull(mapper.lastCriteria().teacher());
        assertNull(mapper.lastCriteria().className());
        assertEquals("Lab 201", mapper.lastCriteria().classroom());
        assertEquals("Networks", mapper.lastCriteria().courseName());
        assertEquals("net", mapper.lastCriteria().keyword());
        assertEquals(3, mapper.lastCriteria().week());
        assertEquals(5000, mapper.lastCriteria().limit());
    }

    @Test
    void appliesDefaultAndMaximumLimits() {
        query.search(criteriaWithLimit(null));
        assertEquals(500, mapper.lastCriteria().limit());

        query.search(criteriaWithLimit(0));
        assertEquals(500, mapper.lastCriteria().limit());

        query.search(criteriaWithLimit(-1));
        assertEquals(500, mapper.lastCriteria().limit());

        query.search(criteriaWithLimit(5001));
        assertEquals(5000, mapper.lastCriteria().limit());

        query.search(criteriaWithLimit(25));
        assertEquals(25, mapper.lastCriteria().limit());
    }

    @Test
    void assemblesSummaryWithAnImmutableSemesterList() {
        LocalDateTime latestSyncAt = LocalDateTime.of(2026, 2, 23, 8, 0);
        mapper.summary = new ScheduleSummaryData(8, 3, 2, 4, latestSyncAt);
        mapper.semesters = new ArrayList<>(List.of("2026-Spring", "2025-Fall"));

        ScheduleSummaryView summary = query.summary();

        assertEquals(8, summary.total());
        assertEquals(3, summary.classCount());
        assertEquals(2, summary.teacherCount());
        assertEquals(4, summary.classroomCount());
        assertEquals(latestSyncAt, summary.latestSyncAt());
        assertEquals(List.of("2026-Spring", "2025-Fall"), summary.semesters());
        assertThrows(UnsupportedOperationException.class, () -> summary.semesters().add("2024-Fall"));
    }

    @Test
    void convertsNullMapperListsToImmutableEmptyLists() {
        mapper.entries = null;
        mapper.semesters = null;
        mapper.summary = null;

        List<ScheduleEntryView> entries = query.search(criteriaWithLimit(10));
        List<String> semesters = query.listSemesters();
        ScheduleSummaryView summary = query.summary();

        assertEquals(List.of(), entries);
        assertEquals(List.of(), semesters);
        assertEquals(List.of(), summary.semesters());
        assertEquals(0, summary.total());
        assertThrows(UnsupportedOperationException.class, () -> entries.add(null));
        assertThrows(UnsupportedOperationException.class, () -> semesters.add("2026-Spring"));
    }

    private ScheduleSearchCriteria criteriaWithLimit(Integer limit) {
        return new ScheduleSearchCriteria(null, null, null, null, null, null, null, limit);
    }

    private static final class FakeTeachingScheduleReadPort implements TeachingScheduleReadPort {
        private ScheduleSearchCriteria lastCriteria;
        private List<ScheduleEntryView> entries = List.of();
        private ScheduleSummaryData summary = new ScheduleSummaryData(0, 0, 0, 0, null);
        private List<String> semesters = List.of();

        @Override
        public org.example.backend.teachingschedule.application.ScheduleCalendarData findActiveCalendar() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<org.example.backend.teachingschedule.application.ScheduleOccupancyRow> findOccupancyCandidates(
                org.example.backend.teachingschedule.application.ScheduleOccupancyCriteria criteria) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ScheduleEntryView> search(ScheduleSearchCriteria criteria) {
            lastCriteria = criteria;
            return entries;
        }

        @Override
        public ScheduleSummaryData summary() {
            return summary;
        }

        @Override
        public List<String> listSemesters() {
            return semesters;
        }

        ScheduleSearchCriteria lastCriteria() {
            return lastCriteria;
        }
    }
}
