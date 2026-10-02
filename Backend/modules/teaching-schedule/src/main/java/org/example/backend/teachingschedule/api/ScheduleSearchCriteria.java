package org.example.backend.teachingschedule.api;

public record ScheduleSearchCriteria(
        String semester,
        String teacher,
        String className,
        String classroom,
        String courseName,
        String keyword,
        Integer week,
        Integer limit
) {
}
