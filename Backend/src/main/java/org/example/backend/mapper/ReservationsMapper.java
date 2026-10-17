/*
 * 实验室预约的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/ReservationsMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.ReservationsEntity;

import java.util.List;

@Mapper
public interface ReservationsMapper {


    /**
     * 插入预约。
     * @param reservationsEntity 预约Entity参数。
     */
    void insertReservation(ReservationsEntity reservationsEntity);

    /**
     * 获取预约。
     * @return 当前操作得到的记录集合。
     */
    List<ReservationsEntity> getReservations();

    /**
     * 获取预约。
     * @param id 标识参数。
     * @return ReservationsEntity 类型的处理结果。
     */
    ReservationsEntity getReservation(@Param("id") Integer id);

    /**
     * 统计Conflicting预约。
     * @param id 标识参数。
     * @param reservationsEntity 预约Entity参数。
     * @return int 类型的处理结果。
     */
    int countConflictingReservations(@Param("id") Integer id, @Param("reservation") ReservationsEntity reservationsEntity);

    /**
     * 统计课表Conflicts。
     * @param reservationsEntity 预约Entity参数。
     * @param week 周次参数。
     * @param weekday weekday参数。
     * @return int 类型的处理结果。
     */
    int countTimetableConflicts(
            @Param("reservation") ReservationsEntity reservationsEntity,
            @Param("week") Integer week,
            @Param("weekday") String weekday
    );

    /**
     * 统计当前Approved预约By实验室标识。
     * @param labId 实验室标识参数。
     * @return int 类型的处理结果。
     */
    int countCurrentApprovedReservationsByLabId(@Param("labId") Long labId);

    /**
     * 统计UpcomingApproved预约By实验室标识。
     * @param labId 实验室标识参数。
     * @return int 类型的处理结果。
     */
    int countUpcomingApprovedReservationsByLabId(@Param("labId") Long labId);

    /**
     * 更新预约。
     * @param id 标识参数。
     * @param reservationsEntity 预约Entity参数。
     * @return int 类型的处理结果。
     */
    int updateReservation(@Param("id") Integer id, @Param("reservation") ReservationsEntity reservationsEntity);

    /**
     * 删除预约。
     * @param id 标识参数。
     */
    void deleteReservation(@Param("id") Integer id);

    /**
     * 删除All预约。
     */
    void deleteAllReservations();

    /**
     * 获取Approved。
     * @param id 标识参数。
     * @param reservationsEntity 预约Entity参数。
     * @return int 类型的处理结果。
     */
    int getApproved(@Param("id") Integer id, @Param("reservation") ReservationsEntity reservationsEntity);

    /**
     * 获取Rejected。
     * @param id 标识参数。
     * @param reservationsEntity 预约Entity参数。
     * @return int 类型的处理结果。
     */
    int getRejected(@Param("id") Integer id, @Param("reservation") ReservationsEntity reservationsEntity);

    /**
     * 更新Review状态。
     * @param id 标识参数。
     * @param status 状态参数。
     * @param reviewerName reviewer名称参数。
     * @param reviewRemark reviewRemark参数。
     * @return int 类型的处理结果。
     */
    int updateReviewStatus(
            @Param("id") Integer id,
            @Param("status") String status,
            @Param("reviewerName") String reviewerName,
            @Param("reviewRemark") String reviewRemark
    );
}
