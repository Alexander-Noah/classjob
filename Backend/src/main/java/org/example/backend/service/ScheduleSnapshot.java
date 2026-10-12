package org.example.backend.service;

import java.time.Instant;

public record ScheduleSnapshot(Long teachingTaskId, String courseName, String teacherName, Instant startsAt, Instant endsAt) {
}
