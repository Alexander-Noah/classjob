/*
 * AI 助手能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.Map;

public interface AiAssistantUserConfigService {
    Map<String, Object> getConfig(Integer userId);

    Map<String, Object> saveConfig(Integer userId, Map<String, Object> payload);

    Map<String, Object> testConnection(Integer userId);
    Map<String, Object> testConnection(Integer userId, Map<String, Object> payload);

    String chat(Integer userId, String question);

    String chatWithContext(Integer userId, String instruction, String question);
}
