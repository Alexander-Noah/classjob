package org.example.backend.service;

import org.example.backend.service.TimeRange;

import java.time.LocalDate;
public record ReservationAvailabilityRequest(
        Long labId,
        LocalDate reservationDate,
        String normalizedTimeRange,
        TimeRange timeRange,
        Integer attendeeCount,
        Long excludingReservationId
) {
    public ReservationAvailabilityRequest {
        if (labId == null || labId <= 0) {
            throw new IllegalArgumentException("Reservation availability requires a positive laboratory id");
        }
        if (reservationDate == null) {
            throw new IllegalArgumentException("Reservation availability requires a date");
        }
        if (normalizedTimeRange == null || normalizedTimeRange.isBlank()) {
            throw new IllegalArgumentException("Reservation availability requires a time range");
        }
        if (timeRange == null) {
            throw new IllegalArgumentException("Reservation availability requires a time range");
        }
        if (attendeeCount == null || attendeeCount <= 0) {
            throw new IllegalArgumentException("Reservation availability requires a positive attendee count");
        }
    }
}
