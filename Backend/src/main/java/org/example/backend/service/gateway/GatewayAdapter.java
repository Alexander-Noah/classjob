/*
 * 本地网关能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service.gateway;

public interface GatewayAdapter {
    String protocol();

    GatewayCommandResult sendCommand(GatewayCommandRequest request);

    GatewayStatusResult queryStatus(GatewayCommandRequest request);
}
