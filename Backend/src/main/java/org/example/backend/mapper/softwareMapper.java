/*
 * 软件环境的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/softwareMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.backend.entity.softwareEntity;

import java.util.List;

@Mapper
public interface softwareMapper {
    /**
     * 获取实验室Software。
     * @return 当前操作得到的记录集合。
     */
    List<softwareEntity> getLabSoftware();

    /**
     * Inserter实验室Software的处理逻辑。
     * @param software 软件环境参数。
     */
    void InserterLabSoftware(softwareEntity software);

    /**
     * 更新实验室Software。
     * @param software 软件环境参数。
     */
    void updateLabSoftware(softwareEntity software);

    /**
     * 删除实验室Software。
     * @param id 标识参数。
     */
    void deleteLabSoftware(Long id);
}
