package org.example.backend.service;

import java.time.LocalDateTime;

public record ScheduleSummaryData(
        long total,
        long classCount,
        long teacherCount,
        long classroomCount,
        LocalDateTime latestSyncAt
) {
}
