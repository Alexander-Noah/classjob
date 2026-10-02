package org.example.backend.teachingschedule.api;

import java.time.Instant;

public record ScheduleSnapshotView(Long teachingTaskId, String courseName, String teacherName, Instant startsAt, Instant endsAt) {
}
