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
    int deleteByUserAndConversationId(@Param("userId") Integer userId, @Param("conversationId") String conversationId);

    List<AiConversationEntity> listByUser(@Param("userId") Integer userId);

    int upsertConversation(AiConversationEntity conversation);

    int trimUserConversations(@Param("userId") Integer userId, @Param("maxCount") int maxCount);
}
