package org.example.backend.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.backend.service.ReservationOverlapCheck;
import org.example.backend.service.ReservationRecord;
import org.example.backend.service.ReservationRecordScope;
import org.example.backend.service.ReservationReviewUpdate;

import java.util.List;

public interface ReservationPersistenceMapper {
    List<ReservationRecord> findAll(@Param("scope") ReservationRecordScope scope);

    ReservationRecord findOne(@Param("id") long id, @Param("scope") ReservationRecordScope scope);

    int insert(ReservationInsertRow row);

    ReservationOverlapCount countActiveOverlaps(@Param("check") ReservationOverlapCheck check);

    int updateReview(@Param("update") ReservationReviewUpdate update);
}
