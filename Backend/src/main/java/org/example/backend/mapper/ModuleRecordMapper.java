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
    /**
     * 列出记录JSON。
     * @param moduleName module名称参数。
     * @return 当前操作得到的记录集合。
     */
    List<String> listRecordJson(@Param("moduleName") String moduleName);

    /**
     * 删除ByModule名称。
     * @param moduleName module名称参数。
     */
    void deleteByModuleName(@Param("moduleName") String moduleName);

    /**
     * 插入记录。
     * @param moduleName module名称参数。
     * @param recordKey 记录键参数。
     * @param recordJson 记录JSON参数。
     */
    void insertRecord(
            @Param("moduleName") String moduleName,
            @Param("recordKey") String recordKey,
            @Param("recordJson") String recordJson
    );
}
