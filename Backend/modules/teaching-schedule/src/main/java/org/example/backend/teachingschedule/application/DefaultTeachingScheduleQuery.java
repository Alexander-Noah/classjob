package org.example.backend.teachingschedule.application;

import org.example.backend.teachingschedule.api.ScheduleEntryView;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;
import org.example.backend.teachingschedule.api.ScheduleSummaryView;
import org.example.backend.teachingschedule.api.TeachingScheduleQuery;
import org.example.backend.teachingschedule.application.port.out.ScheduleSummaryData;
import org.example.backend.teachingschedule.application.port.out.TeachingScheduleReadPort;

import java.util.List;

public final class DefaultTeachingScheduleQuery implements TeachingScheduleQuery {
    static final int DEFAULT_LIMIT = 500;
    static final int MAX_LIMIT = 5000;

    private final TeachingScheduleReadPort readPort;

    public DefaultTeachingScheduleQuery(TeachingScheduleReadPort readPort) {
        this.readPort = readPort;
    }

    @Override
    public List<ScheduleEntryView> search(ScheduleSearchCriteria criteria) {
        ScheduleSearchCriteria normalized = new ScheduleSearchCriteria(
                trimToNull(criteria.semester()),
                trimToNull(criteria.teacher()),
                trimToNull(criteria.className()),
                trimToNull(criteria.classroom()),
                trimToNull(criteria.courseName()),
                trimToNull(criteria.keyword()),
                criteria.week(),
                normalizeLimit(criteria.limit())
        );
        return immutableList(readPort.search(normalized));
    }

    @Override
    public ScheduleSummaryView summary() {
        ScheduleSummaryData row = readPort.summary();
        List<String> semesters = immutableList(readPort.listSemesters());
        if (row == null) {
            return new ScheduleSummaryView(0, 0, 0, 0, null, semesters);
        }
        return new ScheduleSummaryView(
                row.total(),
                row.classCount(),
                row.teacherCount(),
                row.classroomCount(),
                row.latestSyncAt(),
                semesters
        );
    }

    @Override
    public List<String> listSemesters() {
        return immutableList(readPort.listSemesters());
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private <T> List<T> immutableList(List<T> values) {
        return values == null || values.isEmpty() ? List.of() : List.copyOf(values);
    }
}
