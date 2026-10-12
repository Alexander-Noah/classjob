package org.example.backend.service;

public interface ReservationAvailabilityEvaluator {
    boolean hasConflict(ReservationAvailabilityRequest request);
}
