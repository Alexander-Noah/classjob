/*
 * 通义千问能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.Map;
import java.util.List;

public interface TongyiAgentService {
    Map<String, Object> getConfig();

    Map<String, Object> updateConfig(Map<String, Object> payload);

    boolean isConfigured();

    String callAgent(String question, String sessionId);

    String callAgent(String question, String sessionId, List<Map<String, Object>> images);
}
