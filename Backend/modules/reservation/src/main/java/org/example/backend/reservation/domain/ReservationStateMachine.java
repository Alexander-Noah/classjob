package org.example.backend.reservation.domain;

public final class ReservationStateMachine {
    private ReservationStateMachine() {
    }

    public static ReservationStatus initialStatus(boolean hasConflict) {
        return hasConflict ? ReservationStatus.CONFLICT : ReservationStatus.PENDING;
    }

    public static ReservationStatus approve(ReservationStatus status) {
        return transition(status, ReservationStatus.APPROVED);
    }

    public static ReservationStatus reject(ReservationStatus status) {
        return transition(status, ReservationStatus.REJECTED);
    }

    public static ReservationStatus expire(ReservationStatus status) {
        return transition(status, ReservationStatus.EXPIRED);
    }

    public static ReservationStatus transition(ReservationStatus current, ReservationStatus target) {
        if (current == null || target == null) {
            throw new IllegalArgumentException("Reservation status transition requires current and target statuses");
        }
        if (current == ReservationStatus.PENDING
                && (target == ReservationStatus.APPROVED
                || target == ReservationStatus.REJECTED
                || target == ReservationStatus.EXPIRED)) {
            return target;
        }
        if (current == ReservationStatus.CONFLICT && target == ReservationStatus.REJECTED) {
            return target;
        }
        throw new IllegalStateException("Unsupported reservation status transition: "
                + current.storageValue() + " -> " + target.storageValue());
    }
}
