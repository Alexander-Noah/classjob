/*
 * AI 助手能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.List;
import java.util.Map;

public interface AiAssistantService {
    String generateAnswer(String question, String scene);

    String generateAnswer(String question, String scene, List<Map<String, Object>> images);
}
