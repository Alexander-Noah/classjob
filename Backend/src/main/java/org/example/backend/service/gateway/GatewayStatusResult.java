/*
 * 本地网关数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service.gateway;

import java.time.LocalDateTime;
import java.util.Map;

public record GatewayStatusResult(
        boolean online,
        String runtimeStatus,
        String gatewayMode,
        String responseSummary,
        Map<String, Object> responseResult,
        LocalDateTime reportedAt
) {
    /**
     * 构造 GatewayStatusResult，初始化当前实例。
     */
    public GatewayStatusResult {
        responseResult = responseResult == null ? Map.of() : Map.copyOf(responseResult);
        reportedAt = reportedAt == null ? LocalDateTime.now() : reportedAt;
    }
}
