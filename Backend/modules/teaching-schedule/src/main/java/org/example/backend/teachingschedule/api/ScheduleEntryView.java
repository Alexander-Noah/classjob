package org.example.backend.teachingschedule.api;

import java.time.LocalDateTime;

public record ScheduleEntryView(
        Long id,
        String semester,
        String rowClassName,
        String className,
        String weekday,
        String sectionCode,
        String sectionText,
        Integer startSection,
        Integer endSection,
        String courseName,
        String teacher,
        String weekRaw,
        String weekText,
        String weekExpanded,
        String classroom,
        LocalDateTime createTime
) {
}
