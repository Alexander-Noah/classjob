package org.example.backend.reservation.application.port.out;

import java.util.Objects;
import java.util.Set;

public record ReservationRecordScope(Long applicantUserId, Set<Long> laboratoryIds) {
    public ReservationRecordScope {
        if (applicantUserId != null && laboratoryIds != null) {
            throw new IllegalArgumentException("A reservation scope may target an applicant or laboratories, not both");
        }
        if (laboratoryIds != null) {
            laboratoryIds = Set.copyOf(laboratoryIds);
        }
    }

    public static ReservationRecordScope all() {
        return new ReservationRecordScope(null, null);
    }

    public static ReservationRecordScope forApplicant(long applicantUserId) {
        return new ReservationRecordScope(applicantUserId, null);
    }

    public static ReservationRecordScope forLaboratoryIds(Set<Long> laboratoryIds) {
        return new ReservationRecordScope(null, Objects.requireNonNull(laboratoryIds, "Laboratory IDs are required"));
    }
}
