package org.example.backend.service;

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
