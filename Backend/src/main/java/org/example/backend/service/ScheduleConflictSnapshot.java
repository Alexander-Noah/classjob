package org.example.backend.service;

import java.time.LocalDate;

public record ScheduleConflictSnapshot(Long labId, LocalDate reservationDate, TimeRange timeRange, boolean hasConflict) {
}
