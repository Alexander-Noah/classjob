/*
 * 调课申请的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/ScheduleAdjustmentMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.ScheduleAdjustmentEntity;

import java.util.List;

@Mapper
public interface ScheduleAdjustmentMapper {
    /**
     * 列出Adjustments。
     * @return 当前操作得到的记录集合。
     */
    List<ScheduleAdjustmentEntity> listAdjustments();

    /**
     * 获取Adjustment。
     * @param id 标识参数。
     * @return ScheduleAdjustmentEntity 类型的处理结果。
     */
    ScheduleAdjustmentEntity getAdjustment(@Param("id") Long id);

    /**
     * 插入Adjustment。
     * @param adjustment adjustment参数。
     * @return int 类型的处理结果。
     */
    int insertAdjustment(ScheduleAdjustmentEntity adjustment);

    /**
     * 更新Adjustment。
     * @param id 标识参数。
     * @param adjustment adjustment参数。
     * @return int 类型的处理结果。
     */
    int updateAdjustment(@Param("id") Long id, @Param("adjustment") ScheduleAdjustmentEntity adjustment);

    /**
     * patchAdjustment的处理逻辑。
     * @param id 标识参数。
     * @param status 状态参数。
     * @param tagType tag类型参数。
     * @param flowStep flowStep参数。
     * @return int 类型的处理结果。
     */
    int patchAdjustment(@Param("id") Long id, @Param("status") String status, @Param("tagType") String tagType, @Param("flowStep") Integer flowStep);
}
