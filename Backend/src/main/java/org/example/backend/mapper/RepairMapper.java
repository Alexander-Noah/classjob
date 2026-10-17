/*
 * 报修的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/RepairMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.RepairEntity;

import java.util.List;

@Mapper
public interface RepairMapper {
    /**
     * 列出Repairs。
     * @return 当前操作得到的记录集合。
     */
    List<RepairEntity> listRepairs();

    /**
     * 获取报修。
     * @param id 标识参数。
     * @return RepairEntity 类型的处理结果。
     */
    RepairEntity getRepair(@Param("id") String id);

    /**
     * 插入报修。
     * @param repair 报修参数。
     * @return int 类型的处理结果。
     */
    int insertRepair(RepairEntity repair);

    /**
     * 统计OpenRepairByDescriptionMarker。
     * @param marker marker参数。
     * @return int 类型的处理结果。
     */
    int countOpenRepairByDescriptionMarker(@Param("marker") String marker);

    /**
     * 更新报修。
     * @param id 标识参数。
     * @param repair 报修参数。
     * @return int 类型的处理结果。
     */
    int updateRepair(@Param("id") String id, @Param("repair") RepairEntity repair);

    /**
     * patchRepair的处理逻辑。
     * @param id 标识参数。
     * @param repair 报修参数。
     * @return int 类型的处理结果。
     */
    int patchRepair(@Param("id") String id, @Param("repair") RepairEntity repair);

    /**
     * 删除报修。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int deleteRepair(@Param("id") String id);

    /**
     * 删除AllRepairs。
     * @return int 类型的处理结果。
     */
    int deleteAllRepairs();
}
