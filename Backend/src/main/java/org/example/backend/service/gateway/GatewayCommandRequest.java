/*
 * 本地网关数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service.gateway;

import java.util.Map;

public record GatewayCommandRequest(
        Long gatewayId,
        String gatewayCode,
        String protocolType,
        String gatewayAddress,
        String authType,
        Long pointId,
        String pointCode,
        String requestPath,
        String requestTemplate,
        Long iotDeviceId,
        String iotCode,
        Long labId,
        String deviceType,
        String commandType,
        String action,
        Map<String, Object> payload
) {
    /**
     * 构造 GatewayCommandRequest，初始化当前实例。
     */
    public GatewayCommandRequest {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    /**
     * mock的处理逻辑。
     * @param iotDeviceId 物联设备标识参数。
     * @param iotCode 物联编码参数。
     * @param labId 实验室标识参数。
     * @param deviceType 设备类型参数。
     * @param action action参数。
     * @param payload 载荷参数。
     * @return GatewayCommandRequest 类型的处理结果。
     */
    public static GatewayCommandRequest mock(
            Long iotDeviceId,
            String iotCode,
            Long labId,
            String deviceType,
            String action,
            Map<String, Object> payload
    ) {
        return new GatewayCommandRequest(
                null,
                "mock-gateway",
                "mock",
                "",
                "none",
                null,
                "",
                "",
                "",
                iotDeviceId,
                iotCode,
                labId,
                deviceType,
                action,
                action,
                payload
        );
    }
}
