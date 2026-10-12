package org.example.backend.reservation.application;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.reservation.application.NewReservationRecord;
import org.example.backend.reservation.application.ReservationOverlapCheck;
import org.example.backend.reservation.application.ReservationPersistencePort;
import org.example.backend.reservation.application.ReservationRecord;
import org.example.backend.reservation.application.ReservationRecordScope;
import org.example.backend.reservation.application.ReservationReviewUpdate;
import org.example.backend.reservation.domain.ReservationStatus;
import org.example.backend.reservation.domain.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultReservationAvailabilityEvaluatorTest {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 5);
    private static final TimeRange RANGE = new TimeRange(Instant.parse("2026-10-05T01:00:00Z"), Instant.parse("2026-10-05T03:00:00Z"));

    @Test
    void failsClosedForUnavailableMissingOrMismatchedLaboratoryCapability() {
        for (LabAvailabilityPort labs : List.<LabAvailabilityPort>of(
                (labId, attendees, range) -> CapabilityResult.unavailable("down"),
                (labId, attendees, range) -> CapabilityResult.available(null),
                (labId, attendees, range) -> CapabilityResult.available(new LabAvailabilitySnapshot(9L, 40, true))
        )) {
            assertThrows(IllegalStateException.class, () -> evaluator(labs, clearSchedule(), 0).hasConflict(request(null)));
        }
    }

    @Test
    void reportsLaboratoryUnavailableOrUnderCapacityAsConflict() {
        assertTrue(evaluator((lab, count, range) -> CapabilityResult.available(new LabAvailabilitySnapshot(lab, 40, false)), clearSchedule(), 0)
                .hasConflict(request(null)));
        assertTrue(evaluator((lab, count, range) -> CapabilityResult.available(new LabAvailabilitySnapshot(lab, 29, true)), clearSchedule(), 0)
                .hasConflict(request(null)));
    }

    @Test
    void failsClosedForUnavailableMissingOrMismatchedScheduleCapability() {
        for (ScheduleConflictPort schedules : List.<ScheduleConflictPort>of(
                (labId, date, range) -> CapabilityResult.unavailable("down"),
                (labId, date, range) -> CapabilityResult.available(null),
                (labId, date, range) -> CapabilityResult.available(new ScheduleConflictSnapshot(9L, date, range, false)),
                (labId, date, range) -> CapabilityResult.available(new ScheduleConflictSnapshot(labId, date.plusDays(1), range, false)),
                (labId, date, range) -> CapabilityResult.available(new ScheduleConflictSnapshot(labId, date, new TimeRange(range.startsAt(), range.endsAt().plusSeconds(1)), false))
        )) {
            assertThrows(IllegalStateException.class, () -> evaluator(availableLab(), schedules, 0).hasConflict(request(null)));
        }
    }

    @Test
    void reportsScheduleConflictAndActiveOverlapsAsConflict() {
        assertTrue(evaluator(availableLab(), (lab, date, range) -> CapabilityResult.available(new ScheduleConflictSnapshot(lab, date, range, true)), 0)
                .hasConflict(request(null)));
        assertTrue(evaluator(availableLab(), clearSchedule(), 1).hasConflict(request(null)));
    }

    @Test
    void passesCanonicalRequestAndTerminalExclusionsToOverlapCheck() {
        RecordingReservations reservations = new RecordingReservations(0);
        DefaultReservationAvailabilityEvaluator evaluator = new DefaultReservationAvailabilityEvaluator(reservations, availableLab(), clearSchedule());

        assertFalse(evaluator.hasConflict(request(41L)));

        assertEquals(new ReservationOverlapCheck(7L, DATE, "09:00-11:00",
                Set.of(ReservationStatus.REJECTED, ReservationStatus.CANCELLED, ReservationStatus.EXPIRED, ReservationStatus.COMPLETED), 41L),
                reservations.check);
    }

    @Test
    void rejectsNegativeOverlapCount() {
        assertThrows(IllegalStateException.class, () -> evaluator(availableLab(), clearSchedule(), -1).hasConflict(request(null)));
    }

    @Test
    void requestRequiresValidatedFields() {
        assertThrows(IllegalArgumentException.class, () -> new ReservationAvailabilityRequest(0L, DATE, "09:00-11:00", RANGE, 30, null));
        assertThrows(IllegalArgumentException.class, () -> new ReservationAvailabilityRequest(7L, null, "09:00-11:00", RANGE, 30, null));
        assertThrows(IllegalArgumentException.class, () -> new ReservationAvailabilityRequest(7L, DATE, "09:00-11:00", RANGE, 0, null));
    }

    private ReservationAvailabilityRequest request(Long excludingId) {
        return new ReservationAvailabilityRequest(7L, DATE, "09:00-11:00", RANGE, 30, excludingId);
    }

    private DefaultReservationAvailabilityEvaluator evaluator(LabAvailabilityPort labs, ScheduleConflictPort schedules, long overlaps) {
        return new DefaultReservationAvailabilityEvaluator(new RecordingReservations(overlaps), labs, schedules);
    }

    private LabAvailabilityPort availableLab() {
        return (labId, attendees, range) -> CapabilityResult.available(new LabAvailabilitySnapshot(labId, 40, true));
    }

    private ScheduleConflictPort clearSchedule() {
        return (labId, date, range) -> CapabilityResult.available(new ScheduleConflictSnapshot(labId, date, range, false));
    }

    private static final class RecordingReservations implements ReservationPersistencePort {
        private final long overlaps;
        private ReservationOverlapCheck check;

        private RecordingReservations(long overlaps) { this.overlaps = overlaps; }
        @Override public List<ReservationRecord> findAll(ReservationRecordScope scope) { throw new UnsupportedOperationException(); }
        @Override public Optional<ReservationRecord> findOne(long id, ReservationRecordScope scope) { throw new UnsupportedOperationException(); }
        @Override public long insert(NewReservationRecord record) { throw new UnsupportedOperationException(); }
        @Override public long countActiveOverlaps(ReservationOverlapCheck value) { check = value; return overlaps; }
        @Override public boolean updateReview(ReservationReviewUpdate update) { throw new UnsupportedOperationException(); }
    }
}
