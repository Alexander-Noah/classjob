/*
 * 调课申请能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.ScheduleAdjustmentEntity;

import java.util.List;

public interface ScheduleAdjustmentService {
    /**
     * 列出Adjustments。
     * @return 当前操作得到的记录集合。
     */
    List<ScheduleAdjustmentEntity> listAdjustments();

    /**
     * 创建Adjustment。
     * @param adjustment adjustment参数。
     * @return ScheduleAdjustmentEntity 类型的处理结果。
     */
    ScheduleAdjustmentEntity createAdjustment(ScheduleAdjustmentEntity adjustment);

    /**
     * 更新Adjustment。
     * @param id 标识参数。
     * @param adjustment adjustment参数。
     * @return ScheduleAdjustmentEntity 类型的处理结果。
     */
    ScheduleAdjustmentEntity updateAdjustment(Long id, ScheduleAdjustmentEntity adjustment);

    /**
     * 批准Adjustment。
     * @param id 标识参数。
     * @return ScheduleAdjustmentEntity 类型的处理结果。
     */
    ScheduleAdjustmentEntity approveAdjustment(Long id);

    /**
     * 完成Adjustment。
     * @param id 标识参数。
     * @return ScheduleAdjustmentEntity 类型的处理结果。
     */
    ScheduleAdjustmentEntity completeAdjustment(Long id);
}
