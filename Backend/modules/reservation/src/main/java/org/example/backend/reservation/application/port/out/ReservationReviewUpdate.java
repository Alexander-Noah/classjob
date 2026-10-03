package org.example.backend.reservation.application.port.out;

import org.example.backend.reservation.domain.ReservationStatus;

import java.util.Objects;
import java.util.Set;

public record ReservationReviewUpdate(
        long reservationId,
        ReservationStatus status,
        boolean conflictFlag,
        String reviewerName,
        String reviewRemark,
        Set<ReservationStatus> allowedCurrentStatuses
) {
    public ReservationReviewUpdate {
        Objects.requireNonNull(status, "Review status is required");
        allowedCurrentStatuses = Set.copyOf(Objects.requireNonNull(allowedCurrentStatuses, "Allowed statuses are required"));
        if (allowedCurrentStatuses.isEmpty()) {
            throw new IllegalArgumentException("Allowed statuses must not be empty");
        }
    }
}
