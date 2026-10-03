package org.example.backend.reservation.adapter.out.persistence;

import org.apache.ibatis.annotations.Param;
import org.example.backend.reservation.application.port.out.ReservationOverlapCheck;
import org.example.backend.reservation.application.port.out.ReservationRecord;
import org.example.backend.reservation.application.port.out.ReservationRecordScope;
import org.example.backend.reservation.application.port.out.ReservationReviewUpdate;

import java.util.List;

public interface ReservationPersistenceMapper {
    List<ReservationRecord> findAll(@Param("scope") ReservationRecordScope scope);

    ReservationRecord findOne(@Param("id") long id, @Param("scope") ReservationRecordScope scope);

    int insert(ReservationInsertRow row);

    ReservationOverlapCount countActiveOverlaps(@Param("check") ReservationOverlapCheck check);

    int updateReview(@Param("update") ReservationReviewUpdate update);
}
