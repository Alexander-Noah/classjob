package org.example.backend.teachingschedule.adapter.out.persistence;

import java.time.LocalDateTime;

public record ScheduleSummaryRow(
        long total,
        long classCount,
        long teacherCount,
        long classroomCount,
        LocalDateTime latestSyncAt
) {
}
