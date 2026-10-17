/*
 * 本地网关实现模块。
 */
package org.example.backend.service.gateway;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class MqttGatewayAdapter implements GatewayAdapter {
    /**
     * 协议的处理逻辑。
     * @return String 类型的处理结果。
     */
    @Override
    public String protocol() {
        return "mqtt";
    }

    /**
     * 发送命令。
     * @param request 请求参数。
     * @return GatewayCommandResult 类型的处理结果。
     */
    @Override
    public GatewayCommandResult sendCommand(GatewayCommandRequest request) {
        return GatewayCommandResult.realGatewayNotConfigured("real_gateway_unconfigured", request);
    }

    /**
     * 查询状态。
     * @param request 请求参数。
     * @return GatewayStatusResult 类型的处理结果。
     */
    @Override
    public GatewayStatusResult queryStatus(GatewayCommandRequest request) {
        return new GatewayStatusResult(
                false,
                "unconfigured",
                "real_gateway_unconfigured",
                "待配置真实网关",
                Map.of("protocol", protocol()),
                LocalDateTime.now()
        );
    }
}
