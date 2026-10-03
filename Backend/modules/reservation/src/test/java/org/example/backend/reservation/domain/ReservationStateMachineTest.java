package org.example.backend.reservation.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationStateMachineTest {
    @Test
    void derivesCreationStatusFromConflict() {
        assertEquals(ReservationStatus.PENDING, ReservationStateMachine.initialStatus(false));
        assertEquals(ReservationStatus.CONFLICT, ReservationStateMachine.initialStatus(true));
    }

    @Test
    void allowsPendingToApprovedRejectedAndExpired() {
        assertEquals(ReservationStatus.APPROVED, ReservationStateMachine.approve(ReservationStatus.PENDING));
        assertEquals(ReservationStatus.REJECTED, ReservationStateMachine.reject(ReservationStatus.PENDING));
        assertEquals(ReservationStatus.EXPIRED, ReservationStateMachine.expire(ReservationStatus.PENDING));
    }

    @Test
    void allowsConflictToRejectedOnly() {
        assertEquals(ReservationStatus.REJECTED, ReservationStateMachine.reject(ReservationStatus.CONFLICT));
        assertThrows(IllegalStateException.class, () -> ReservationStateMachine.approve(ReservationStatus.CONFLICT));
        assertThrows(IllegalStateException.class, () -> ReservationStateMachine.expire(ReservationStatus.CONFLICT));
    }

    @Test
    void rejectsApprovalOfEveryFinalStatus() {
        for (ReservationStatus status : ReservationStatus.values()) {
            if (status.isFinal()) {
                assertThrows(IllegalStateException.class, () -> ReservationStateMachine.approve(status));
            }
        }
    }

    @Test
    void failsExplicitlyForUnknownOrMissingStoredStatus() {
        assertEquals(ReservationStatus.PENDING, ReservationStatus.fromStorageValue("待审核"));
        assertThrows(IllegalArgumentException.class, () -> ReservationStatus.fromStorageValue("未知"));
        assertThrows(IllegalArgumentException.class, () -> ReservationStatus.fromStorageValue(null));
    }
}
