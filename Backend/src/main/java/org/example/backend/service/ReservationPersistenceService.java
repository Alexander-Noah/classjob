package org.example.backend.service;

import org.example.backend.mapper.ReservationInsertRow;
import org.example.backend.mapper.ReservationOverlapCount;
import org.example.backend.mapper.ReservationPersistenceMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public final class ReservationPersistenceService {
    private final ReservationPersistenceMapper mapper;

    public ReservationPersistenceService(ReservationPersistenceMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public List<ReservationRecord> findAll(ReservationRecordScope scope) {
        List<ReservationRecord> records = mapper.findAll(Objects.requireNonNull(scope, "scope"));
        return records == null ? List.of() : records;
    }

    public Optional<ReservationRecord> findOne(long id, ReservationRecordScope scope) {
        return Optional.ofNullable(mapper.findOne(id, Objects.requireNonNull(scope, "scope")));
    }

    public long insert(NewReservationRecord record) {
        ReservationInsertRow row = new ReservationInsertRow(Objects.requireNonNull(record, "record"));
        mapper.insert(row);
        if (row.getId() == null) {
            throw new IllegalStateException("Reservation insert did not return a generated ID");
        }
        return row.getId();
    }

    public long countActiveOverlaps(ReservationOverlapCheck check) {
        ReservationOverlapCount count = mapper.countActiveOverlaps(Objects.requireNonNull(check, "check"));
        if (count == null) {
            throw new IllegalStateException("Reservation overlap count was not returned");
        }
        return count.value();
    }

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
