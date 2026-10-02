package org.example.backend.teachingschedule.api;

import java.time.LocalDateTime;
import java.util.List;

public record ScheduleSummaryView(
        long total,
        long classCount,
        long teacherCount,
        long classroomCount,
        LocalDateTime latestSyncAt,
        List<String> semesters
) {
    public ScheduleSummaryView {
        semesters = semesters == null ? List.of() : List.copyOf(semesters);
    }
}
