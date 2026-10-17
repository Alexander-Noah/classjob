/*
 * AI 会话历史能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.List;
import java.util.Map;

public interface AiConversationService {
    void deleteConversation(Integer userId, String conversationId);

    List<Map<String, Object>> listConversations(Integer userId);

    List<Map<String, Object>> saveConversations(Integer userId, List<Map<String, Object>> conversations);
}
