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
    List<softwareEntity> getLabSoftware();

    void InserterLabSoftware(softwareEntity software);

    void updateLabSoftware(softwareEntity software);

    void deleteLabSoftware(Long id);
}
