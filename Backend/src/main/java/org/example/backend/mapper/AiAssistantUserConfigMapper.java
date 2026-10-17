/*
 * AI 助手的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/AiAssistantUserConfigMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.AiAssistantUserConfigEntity;

@Mapper
public interface AiAssistantUserConfigMapper {
    /**
     * 获取By用户标识。
     * @param userId 用户标识参数。
     * @return AiAssistantUserConfigEntity 类型的处理结果。
     */
    AiAssistantUserConfigEntity getByUserId(@Param("userId") Integer userId);

    /**
     * upsert配置的处理逻辑。
     * @param config 配置参数。
     * @return int 类型的处理结果。
     */
    int upsertConfig(AiAssistantUserConfigEntity config);
}
