package org.example.backend.teachingschedule.application;

import java.time.LocalDateTime;

public record ScheduleSummaryData(
        long total,
        long classCount,
        long teacherCount,
        long classroomCount,
        LocalDateTime latestSyncAt
) {
}
