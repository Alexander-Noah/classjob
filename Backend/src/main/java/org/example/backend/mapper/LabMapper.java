/*
 * 实验室的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/LabMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.backend.VO.LabOptionVO;
import org.example.backend.entity.LabEntity;

import java.util.List;

@Mapper
public interface LabMapper {
    /**
     * 获取实验室。
     * @return 当前操作得到的记录集合。
     */
    List<LabEntity> getLabs();

    /**
     * 获取实验室ByManager用户标识。
     * @param managerUserId manager用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<LabEntity> getLabsByManagerUserId(@Param("managerUserId") Integer managerUserId);

    /**
     * 添加实验室。
     * @param lab 实验室参数。
     * @return int 类型的处理结果。
     */
    int addLab(LabEntity lab);

    /**
     * 获取实验室By标识。
     * @param id 标识参数。
     * @return LabEntity 类型的处理结果。
     */
    LabEntity getLabById(Integer id);

    /**
     * 更新实验室。
     * @param id 标识参数。
     * @param lab 实验室参数。
     */
    void updateLab(@Param("id") Integer id, @Param("lab") LabEntity lab);

    /**
     * 更新实验室Open状态。
     * @param id 标识参数。
     * @param openStatus open状态参数。
     * @return int 类型的处理结果。
     */
    int updateLabOpenStatus(@Param("id") Integer id, @Param("openStatus") String openStatus);

    /**
     * 删除RepairsBy实验室标识。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int deleteRepairsByLabId(Integer id);

    /**
     * 删除预约By实验室标识。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int deleteReservationsByLabId(Integer id);

    /**
     * 删除课程环境By实验室标识。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int deleteCourseEnvironmentsByLabId(Integer id);

    /**
     * 删除实验室SoftwareBy实验室标识。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int deleteLabSoftwareByLabId(Integer id);

    /**
     * 删除设备By实验室标识。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int deleteDevicesByLabId(Integer id);

    /**
     * 删除实验室。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int deleteLab(Integer id);

    /**
     * 选择实验室选项。
     * @return 当前操作得到的记录集合。
     */
    List<LabOptionVO> selectLabOptions();
}
