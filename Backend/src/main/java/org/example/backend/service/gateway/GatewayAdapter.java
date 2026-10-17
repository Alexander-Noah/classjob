/*
 * 本地网关能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service.gateway;

public interface GatewayAdapter {
    /**
     * 协议的处理逻辑。
     * @return String 类型的处理结果。
     */
    String protocol();

    /**
     * 发送命令。
     * @param request 请求参数。
     * @return GatewayCommandResult 类型的处理结果。
     */
    GatewayCommandResult sendCommand(GatewayCommandRequest request);

    /**
     * 查询状态。
     * @param request 请求参数。
     * @return GatewayStatusResult 类型的处理结果。
     */
    GatewayStatusResult queryStatus(GatewayCommandRequest request);
}
