/*
 * AI 会话历史能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.List;
import java.util.Map;

public interface AiConversationService {
    /**
     * 删除会话。
     * @param userId 用户标识参数。
     * @param conversationId 会话标识参数。
     */
    void deleteConversation(Integer userId, String conversationId);

    /**
     * 列出会话。
     * @param userId 用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listConversations(Integer userId);

    /**
     * 保存会话。
     * @param userId 用户标识参数。
     * @param conversations 会话参数。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> saveConversations(Integer userId, List<Map<String, Object>> conversations);
}
