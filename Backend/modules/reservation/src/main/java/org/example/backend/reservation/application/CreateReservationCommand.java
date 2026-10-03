package org.example.backend.reservation.application;

import java.time.LocalDate;

public record CreateReservationCommand(
        String applicantName,
        String department,
        String contact,
        Long labId,
        LocalDate reservationDate,
        String timeRange,
        String scene,
        Integer attendeeCount,
        String reason
) {
}
