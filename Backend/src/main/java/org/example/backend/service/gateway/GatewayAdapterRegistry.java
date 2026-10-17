/*
 * 本地网关实现模块。
 */
package org.example.backend.service.gateway;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GatewayAdapterRegistry {
    private final Map<String, GatewayAdapter> adapters;
    private final GatewayAdapter mockGatewayAdapter;

    /**
     * 构造 GatewayAdapterRegistry，初始化当前实例。
     * @param adapters 适配器参数。
     * @param mockGatewayAdapter mock网关适配器参数。
     */
    public GatewayAdapterRegistry(List<GatewayAdapter> adapters, MockGatewayAdapter mockGatewayAdapter) {
        this.adapters = adapters.stream()
                .collect(Collectors.toMap(/** 处理 toMap 调用中的回调逻辑。 入参：adapter。 */ adapter -> normalize(adapter.protocol()), Function.identity(), /** 处理 toMap 调用中的回调逻辑。 入参：left、right。 */ (left, right) -> left));
        this.mockGatewayAdapter = mockGatewayAdapter;
    }

    /**
     * 解析当前操作。
     * @param protocolType 协议类型参数。
     * @param configured configured参数。
     * @return GatewayAdapter 类型的处理结果。
     */
    public GatewayAdapter resolve(String protocolType, boolean configured) {
        String protocol = normalize(protocolType);
        if (!configured || !StringUtils.hasText(protocol) || "mock".equals(protocol)) {
            return mockGatewayAdapter;
        }
        return adapters.getOrDefault(protocol, mockGatewayAdapter);
    }

    /**
     * 归一化当前操作。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().replace("-", "_").toLowerCase(Locale.ROOT);
    }
}
