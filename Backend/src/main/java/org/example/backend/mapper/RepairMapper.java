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
    List<RepairEntity> listRepairs();

    RepairEntity getRepair(@Param("id") String id);

    int insertRepair(RepairEntity repair);

    int countOpenRepairByDescriptionMarker(@Param("marker") String marker);

    int updateRepair(@Param("id") String id, @Param("repair") RepairEntity repair);

    int patchRepair(@Param("id") String id, @Param("repair") RepairEntity repair);

    int deleteRepair(@Param("id") String id);

    int deleteAllRepairs();
}
