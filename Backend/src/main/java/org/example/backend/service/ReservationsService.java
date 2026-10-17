/*
 * 实验室预约能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.ReservationsEntity;

import java.util.List;
import java.util.Map;

public interface ReservationsService {

    /**
     * 获取预约。
     * @return Object 类型的处理结果。
     */
    Object getReservations();

    /**
     * 插入预约。
     * @param reservationsEntity 预约Entity参数。
     */
    void insertReservation(ReservationsEntity reservationsEntity);

    /**
     * 获取预约。
     * @param id 标识参数。
     * @return Object 类型的处理结果。
     */
    Object getReservation(Integer id);

    /**
     * 更新预约。
     * @param id 标识参数。
     * @param reservationsEntity 预约Entity参数。
     */
    void updateReservation(Integer id, ReservationsEntity reservationsEntity);

    /**
     * 删除预约。
     * @param id 标识参数。
     */
    void deleteReservation(Integer id);

    /**
     * replace预约的处理逻辑。
     * @param reservations 预约参数。
     * @return 当前操作得到的记录集合。
     */
    List<ReservationsEntity> replaceReservations(List<ReservationsEntity> reservations);

    /**
     * 获取Approved。
     * @param id 标识参数。
     * @param reservationsEntity 预约Entity参数。
     * @return ReservationsEntity 类型的处理结果。
     */
    ReservationsEntity getApproved(Integer id, ReservationsEntity reservationsEntity);

    /**
     * 获取Rejected。
     * @param id 标识参数。
     * @param reservationsEntity 预约Entity参数。
     * @return ReservationsEntity 类型的处理结果。
     */
    ReservationsEntity getRejected(Integer id, ReservationsEntity reservationsEntity);

    /**
     * 获取Scan预约Profile。
     * @param labId 实验室标识参数。
     * @param labCode 实验室编码参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getScanReservationProfile(Integer labId, String labCode);

    /**
     * 提交Scan预约。
     * @param reservationsEntity 预约Entity参数。
     * @return ReservationsEntity 类型的处理结果。
     */
    ReservationsEntity submitScanReservation(ReservationsEntity reservationsEntity);

    /**
     * 获取Scan预约状态。
     * @param id 标识参数。
     * @param contact contact参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getScanReservationStatus(Integer id, String contact);
}
