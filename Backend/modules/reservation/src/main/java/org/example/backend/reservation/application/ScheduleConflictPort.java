package org.example.backend.reservation.application;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.reservation.domain.TimeRange;

import java.time.LocalDate;

public interface ScheduleConflictPort {
    CapabilityResult<ScheduleConflictSnapshot> check(Long labId, LocalDate reservationDate, TimeRange timeRange);
}
