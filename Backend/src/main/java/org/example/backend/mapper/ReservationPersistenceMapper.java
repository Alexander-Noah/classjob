/*
 * 实验室预约的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/ReservationPersistenceMapper.xml，修改参数或字段需同步接口与 XML。
 */
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
