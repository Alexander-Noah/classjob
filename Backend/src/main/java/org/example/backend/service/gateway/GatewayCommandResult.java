/*
 * 本地网关数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service.gateway;

import java.time.LocalDateTime;
import java.util.Map;

public record GatewayCommandResult(
        boolean success,
        String resultStatus,
        String executionStatus,
        String gatewayMode,
        String responseSummary,
        String errorMessage,
        Map<String, Object> requestParams,
        Map<String, Object> responseResult,
        LocalDateTime executedAt,
        LocalDateTime finishedAt
) {
    /**
     * 构造 GatewayCommandResult，初始化当前实例。
     */
    public GatewayCommandResult {
        requestParams = requestParams == null ? Map.of() : Map.copyOf(requestParams);
        responseResult = responseResult == null ? Map.of() : Map.copyOf(responseResult);
        executedAt = executedAt == null ? LocalDateTime.now() : executedAt;
    }

    /**
     * mock成功的处理逻辑。
     * @param request 请求参数。
     * @param responseResult 响应结果参数。
     * @return GatewayCommandResult 类型的处理结果。
     */
    public static GatewayCommandResult mockSuccess(GatewayCommandRequest request, Map<String, Object> responseResult) {
        LocalDateTime now = LocalDateTime.now();
        return new GatewayCommandResult(
                true,
                "mock_success",
                "completed",
                "mock_gateway",
                "模拟网关演示：命令已由 MockGatewayAdapter 处理，非真实硬件回执",
                "",
                request.payload(),
                responseResult,
                now,
                now
        );
    }

    /**
     * real网关NotConfigured的处理逻辑。
     * @param mode mode参数。
     * @param request 请求参数。
     * @return GatewayCommandResult 类型的处理结果。
     */
    public static GatewayCommandResult realGatewayNotConfigured(String mode, GatewayCommandRequest request) {
        LocalDateTime now = LocalDateTime.now();
        return new GatewayCommandResult(
                false,
                "failed",
                "failed",
                mode,
                "待配置真实网关",
                "真实网关协议、地址、鉴权或点位尚未完成配置",
                request.payload(),
                Map.of(
                        "iotCode", request.iotCode() == null ? "" : request.iotCode(),
                        "action", request.action() == null ? "" : request.action(),
                        "protocol", request.protocolType() == null ? "" : request.protocolType()
                ),
                now,
                now
        );
    }
}
