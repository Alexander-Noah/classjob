/*
 * AI 助手能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.List;
import java.util.Map;

public interface AiAssistantService {
    /**
     * generateAnswer的处理逻辑。
     * @param question question参数。
     * @param scene scene参数。
     * @return String 类型的处理结果。
     */
    String generateAnswer(String question, String scene);

    /**
     * generateAnswer的处理逻辑。
     * @param question question参数。
     * @param scene scene参数。
     * @param images images参数。
     * @return String 类型的处理结果。
     */
    String generateAnswer(String question, String scene, List<Map<String, Object>> images);
}
