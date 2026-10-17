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


    void insertReservation(ReservationsEntity reservationsEntity);

    List<ReservationsEntity> getReservations();

    ReservationsEntity getReservation(@Param("id") Integer id);

    int countConflictingReservations(@Param("id") Integer id, @Param("reservation") ReservationsEntity reservationsEntity);

    int countTimetableConflicts(
            @Param("reservation") ReservationsEntity reservationsEntity,
            @Param("week") Integer week,
            @Param("weekday") String weekday
    );

    int countCurrentApprovedReservationsByLabId(@Param("labId") Long labId);

    int countUpcomingApprovedReservationsByLabId(@Param("labId") Long labId);

    int updateReservation(@Param("id") Integer id, @Param("reservation") ReservationsEntity reservationsEntity);

    void deleteReservation(@Param("id") Integer id);

    void deleteAllReservations();

    int getApproved(@Param("id") Integer id, @Param("reservation") ReservationsEntity reservationsEntity);

    int getRejected(@Param("id") Integer id, @Param("reservation") ReservationsEntity reservationsEntity);

    int updateReviewStatus(
            @Param("id") Integer id,
            @Param("status") String status,
            @Param("reviewerName") String reviewerName,
            @Param("reviewRemark") String reviewRemark
    );
}
