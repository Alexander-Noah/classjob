package org.example.backend.reservation.infrastructure;

import org.example.backend.reservation.application.NewReservationRecord;
import org.example.backend.reservation.application.ReservationOverlapCheck;
import org.example.backend.reservation.application.ReservationPersistencePort;
import org.example.backend.reservation.application.ReservationRecord;
import org.example.backend.reservation.application.ReservationRecordScope;
import org.example.backend.reservation.application.ReservationReviewUpdate;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MyBatisReservationPersistenceAdapter implements ReservationPersistencePort {
    private final ReservationPersistenceMapper mapper;

    public MyBatisReservationPersistenceAdapter(ReservationPersistenceMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public List<ReservationRecord> findAll(ReservationRecordScope scope) {
        List<ReservationRecord> records = mapper.findAll(Objects.requireNonNull(scope, "scope"));
        return records == null ? List.of() : records;
    }

    @Override
    public Optional<ReservationRecord> findOne(long id, ReservationRecordScope scope) {
        return Optional.ofNullable(mapper.findOne(id, Objects.requireNonNull(scope, "scope")));
    }

    @Override
    public long insert(NewReservationRecord record) {
        ReservationInsertRow row = new ReservationInsertRow(Objects.requireNonNull(record, "record"));
        mapper.insert(row);
        if (row.getId() == null) {
            throw new IllegalStateException("Reservation insert did not return a generated ID");
        }
        return row.getId();
    }

    @Override
    public long countActiveOverlaps(ReservationOverlapCheck check) {
        ReservationOverlapCount count = mapper.countActiveOverlaps(Objects.requireNonNull(check, "check"));
        if (count == null) {
            throw new IllegalStateException("Reservation overlap count was not returned");
        }
        return count.value();
    }

    @Override
    public boolean updateReview(ReservationReviewUpdate update) {
        int changed = mapper.updateReview(Objects.requireNonNull(update, "update"));
        if (changed == 0) {
            return false;
        }
        if (changed == 1) {
            return true;
        }
        throw new IllegalStateException("Conditional reservation review update changed " + changed + " rows");
    }
}
