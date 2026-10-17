/*
 * ModuleRecordMapper的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/ModuleRecordMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ModuleRecordMapper {
    List<String> listRecordJson(@Param("moduleName") String moduleName);

    void deleteByModuleName(@Param("moduleName") String moduleName);

    void insertRecord(
            @Param("moduleName") String moduleName,
            @Param("recordKey") String recordKey,
            @Param("recordJson") String recordJson
    );
}
