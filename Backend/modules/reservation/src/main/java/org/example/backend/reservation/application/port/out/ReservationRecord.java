package org.example.backend.reservation.application.port.out;

import org.example.backend.reservation.domain.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationRecord(
        Long id,
        Long applicantUserId,
        String applicantName,
        String department,
        String contact,
        Long labId,
        LocalDate reservationDate,
        String timeRange,
        String scene,
        Integer attendeeCount,
        String reason,
        ReservationStatus status,
        boolean conflictFlag,
        String reviewerName,
        String reviewRemark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
