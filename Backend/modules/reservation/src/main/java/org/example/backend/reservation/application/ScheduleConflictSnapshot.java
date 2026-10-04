package org.example.backend.reservation.application;

import org.example.backend.reservation.domain.TimeRange;

import java.time.LocalDate;

public record ScheduleConflictSnapshot(Long labId, LocalDate reservationDate, TimeRange timeRange, boolean hasConflict) {
}
