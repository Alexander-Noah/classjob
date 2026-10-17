/*
 * 通义千问能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.Map;
import java.util.List;

public interface TongyiAgentService {
    /**
     * 获取配置。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getConfig();

    /**
     * 更新配置。
     * @param payload 载荷参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> updateConfig(Map<String, Object> payload);

    /**
     * 判断是否为Configured。
     * @return 当前判断的布尔结果。
     */
    boolean isConfigured();

    /**
     * callAgent的处理逻辑。
     * @param question question参数。
     * @param sessionId 会话标识参数。
     * @return String 类型的处理结果。
     */
    String callAgent(String question, String sessionId);

    /**
     * callAgent的处理逻辑。
     * @param question question参数。
     * @param sessionId 会话标识参数。
     * @param images images参数。
     * @return String 类型的处理结果。
     */
    String callAgent(String question, String sessionId, List<Map<String, Object>> images);
}
