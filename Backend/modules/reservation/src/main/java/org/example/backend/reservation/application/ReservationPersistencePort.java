package org.example.backend.reservation.application;

import java.util.List;
import java.util.Optional;

public interface ReservationPersistencePort {
    List<ReservationRecord> findAll(ReservationRecordScope scope);

    Optional<ReservationRecord> findOne(long id, ReservationRecordScope scope);

    long insert(NewReservationRecord record);

    long countActiveOverlaps(ReservationOverlapCheck check);

    boolean updateReview(ReservationReviewUpdate update);
}
