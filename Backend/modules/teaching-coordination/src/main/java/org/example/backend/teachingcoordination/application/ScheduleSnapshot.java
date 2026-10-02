package org.example.backend.teachingcoordination.application;

import java.time.Instant;

public record ScheduleSnapshot(Long teachingTaskId, String courseName, String teacherName, Instant startsAt, Instant endsAt) {
}
