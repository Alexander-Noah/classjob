package org.example.backend.service;

import org.example.backend.service.CapabilityResult;
import org.example.backend.service.CapabilityStatus;
import org.example.backend.service.LabAvailabilityPort;
import org.example.backend.service.LabAvailabilitySnapshot;
import org.example.backend.service.ScheduleConflictPort;
import org.example.backend.service.ScheduleConflictSnapshot;
import org.example.backend.entity.ReservationStatus;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class DefaultReservationAvailabilityEvaluator implements ReservationAvailabilityEvaluator {
    private static final Set<ReservationStatus> TERMINAL_OVERLAP_EXCLUSIONS = EnumSet.of(
            ReservationStatus.REJECTED, ReservationStatus.CANCELLED,
            ReservationStatus.EXPIRED, ReservationStatus.COMPLETED
    );

    private final ReservationPersistenceService reservations;
    private final LabAvailabilityPort laboratoryAvailability;
    private final ScheduleConflictPort scheduleConflicts;

    public DefaultReservationAvailabilityEvaluator(ReservationPersistenceService reservations,
                                                   LabAvailabilityPort laboratoryAvailability,
                                                   ScheduleConflictPort scheduleConflicts) {
        this.reservations = Objects.requireNonNull(reservations, "reservations");
        this.laboratoryAvailability = Objects.requireNonNull(laboratoryAvailability, "laboratoryAvailability");
        this.scheduleConflicts = Objects.requireNonNull(scheduleConflicts, "scheduleConflicts");
    }

    @Override
    public boolean hasConflict(ReservationAvailabilityRequest request) {
        ReservationAvailabilityRequest validated = Objects.requireNonNull(request, "request");
        LabAvailabilitySnapshot laboratory = requireLaboratoryCapability(validated);
        ScheduleConflictSnapshot schedule = requireScheduleCapability(validated);
        long overlapCount = reservations.countActiveOverlaps(new ReservationOverlapCheck(
                validated.labId(), validated.reservationDate(), validated.normalizedTimeRange(),
                TERMINAL_OVERLAP_EXCLUSIONS, validated.excludingReservationId()));
        if (overlapCount < 0) {
            throw new IllegalStateException("Reservation overlap count cannot be negative");
        }
        return !laboratory.available() || laboratory.capacity() < validated.attendeeCount()
                || schedule.hasConflict() || overlapCount > 0;
    }

    private LabAvailabilitySnapshot requireLaboratoryCapability(ReservationAvailabilityRequest request) {
        CapabilityResult<LabAvailabilitySnapshot> result = laboratoryAvailability.check(
                request.labId(), request.attendeeCount(), request.timeRange());
        if (result == null || result.status() != CapabilityStatus.AVAILABLE || result.value() == null) {
            throw new IllegalStateException("Laboratory availability capability is unavailable");
        }
        LabAvailabilitySnapshot snapshot = result.value();
        if (!request.labId().equals(snapshot.labId())) {
            throw new IllegalStateException("Laboratory availability capability returned a mismatched laboratory");
        }
        return snapshot;
    }

    private ScheduleConflictSnapshot requireScheduleCapability(ReservationAvailabilityRequest request) {
        CapabilityResult<ScheduleConflictSnapshot> result = scheduleConflicts.check(
                request.labId(), request.reservationDate(), request.timeRange());
        if (result == null || result.status() != CapabilityStatus.AVAILABLE || result.value() == null) {
            throw new IllegalStateException("Schedule conflict capability is unavailable");
        }
        ScheduleConflictSnapshot snapshot = result.value();
        if (!request.labId().equals(snapshot.labId()) || !request.reservationDate().equals(snapshot.reservationDate())
                || !request.timeRange().equals(snapshot.timeRange())) {
            throw new IllegalStateException("Schedule conflict capability returned a mismatched request");
        }
        return snapshot;
    }
}
