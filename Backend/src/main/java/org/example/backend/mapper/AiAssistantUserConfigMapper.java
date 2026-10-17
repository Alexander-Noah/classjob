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
    AiAssistantUserConfigEntity getByUserId(@Param("userId") Integer userId);

    int upsertConfig(AiAssistantUserConfigEntity config);
}
