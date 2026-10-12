package org.example.backend.teachingcoordination.api;

import java.time.Instant;

public record ScheduleSnapshot(Long teachingTaskId, String courseName, String teacherName, Instant startsAt, Instant endsAt) {
}
