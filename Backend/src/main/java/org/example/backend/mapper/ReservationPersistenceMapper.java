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
    /**
     * 查找All。
     * @param scope scope参数。
     * @return 当前操作得到的记录集合。
     */
    List<ReservationRecord> findAll(@Param("scope") ReservationRecordScope scope);

    /**
     * 查找One。
     * @param id 标识参数。
     * @param scope scope参数。
     * @return ReservationRecord 类型的处理结果。
     */
    ReservationRecord findOne(@Param("id") long id, @Param("scope") ReservationRecordScope scope);

    /**
     * 插入当前操作。
     * @param row 行参数。
     * @return int 类型的处理结果。
     */
    int insert(ReservationInsertRow row);

    /**
     * 统计活动Overlaps。
     * @param check check参数。
     * @return ReservationOverlapCount 类型的处理结果。
     */
    ReservationOverlapCount countActiveOverlaps(@Param("check") ReservationOverlapCheck check);

    /**
     * 更新Review。
     * @param update update参数。
     * @return int 类型的处理结果。
     */
    int updateReview(@Param("update") ReservationReviewUpdate update);
}
