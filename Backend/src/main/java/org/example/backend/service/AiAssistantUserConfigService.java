/*
 * AI 助手能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.Map;

public interface AiAssistantUserConfigService {
    /**
     * 获取配置。
     * @param userId 用户标识参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getConfig(Integer userId);

    /**
     * 保存配置。
     * @param userId 用户标识参数。
     * @param payload 载荷参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> saveConfig(Integer userId, Map<String, Object> payload);

    /**
     * 验证连接。
     * @param userId 用户标识参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> testConnection(Integer userId);
    /**
     * 验证连接。
     * @param userId 用户标识参数。
     * @param payload 载荷参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> testConnection(Integer userId, Map<String, Object> payload);

    /**
     * chat的处理逻辑。
     * @param userId 用户标识参数。
     * @param question question参数。
     * @return String 类型的处理结果。
     */
    String chat(Integer userId, String question);

    /**
     * chatWith上下文的处理逻辑。
     * @param userId 用户标识参数。
     * @param instruction instruction参数。
     * @param question question参数。
     * @return String 类型的处理结果。
     */
    String chatWithContext(Integer userId, String instruction, String question);
}
