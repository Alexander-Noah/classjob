package org.example.backend.reservation.application;

public interface ReservationAvailabilityEvaluator {
    boolean hasConflict(ReservationAvailabilityRequest request);
}
