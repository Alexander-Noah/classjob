package org.example.backend.service;

import java.util.List;

public final class DefaultTeachingScheduleQuery implements TeachingScheduleQuery {
    static final int DEFAULT_LIMIT = 500;
    static final int MAX_LIMIT = 5000;

    private final TeachingScheduleReadService readService;

    public DefaultTeachingScheduleQuery(TeachingScheduleReadService readService) {
        this.readService = readService;
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
        return immutableList(readService.search(normalized));
    }

    @Override
    public ScheduleSummaryView summary() {
        ScheduleSummaryData row = readService.summary();
        List<String> semesters = immutableList(readService.listSemesters());
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
        return immutableList(readService.listSemesters());
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
