/*
 * AI 会话历史的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/AiConversationMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.AiConversationEntity;

import java.util.List;

@Mapper
public interface AiConversationMapper {
    /**
     * 删除By用户And会话标识。
     * @param userId 用户标识参数。
     * @param conversationId 会话标识参数。
     * @return int 类型的处理结果。
     */
    int deleteByUserAndConversationId(@Param("userId") Integer userId, @Param("conversationId") String conversationId);

    /**
     * 列出By用户。
     * @param userId 用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<AiConversationEntity> listByUser(@Param("userId") Integer userId);

    /**
     * upsert会话的处理逻辑。
     * @param conversation 会话参数。
     * @return int 类型的处理结果。
     */
    int upsertConversation(AiConversationEntity conversation);

    /**
     * 裁剪用户会话。
     * @param userId 用户标识参数。
     * @param maxCount max数量参数。
     * @return int 类型的处理结果。
     */
    int trimUserConversations(@Param("userId") Integer userId, @Param("maxCount") int maxCount);
}
