package org.example.backend.service;

import java.time.Instant;

public record ScheduleSnapshotView(Long teachingTaskId, String courseName, String teacherName, Instant startsAt, Instant endsAt) {
}
