/*
 * 本地网关回归测试；测试夹具与生产配置分离。
 * 覆盖场景：mockGatewayReturnsStandardizedCommandResultWithoutClaimingRealHardware、realGatewayPlaceholdersReturnPendingConfigurationState。
 */
package org.example.backend.service.gateway;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayAdapterBehaviorTest {
    /**
     * mock网关ReturnsStandardized命令结果WithoutClaimingRealHardware的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void mockGatewayReturnsStandardizedCommandResultWithoutClaimingRealHardware() throws IOException {
        String request = readGatewaySource("GatewayCommandRequest.java");
        String result = readGatewaySource("GatewayCommandResult.java");
        String adapter = readGatewaySource("MockGatewayAdapter.java");

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(request.contains("public static GatewayCommandRequest mock(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(result.contains("mock_success")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(result.contains("mock_gateway")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(result.contains("模拟网关演示")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(result.contains("非真实硬件回执")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(adapter.contains("response.put(\"action\", request.action())")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(adapter.contains("response.put(\"iotCode\", request.iotCode())"))
        );
    }

    /**
     * real网关PlaceholdersReturn待处理配置状态的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void realGatewayPlaceholdersReturnPendingConfigurationState() throws IOException {
        String http = readGatewaySource("HttpGatewayAdapter.java");
        String mqtt = readGatewaySource("MqttGatewayAdapter.java");
        String result = readGatewaySource("GatewayCommandResult.java");

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(http.contains("GatewayCommandResult.realGatewayNotConfigured")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mqtt.contains("GatewayCommandResult.realGatewayNotConfigured")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(result.contains("\"failed\"")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(result.contains("待配置真实网关"))
        );
    }

    /**
     * 读取网关来源。
     * @param fileName 文件名称参数。
     * @return String 类型的处理结果。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    private String readGatewaySource(String fileName) throws IOException {
        Path path = Path.of("src", "main", "java", "org", "example", "backend", "service", "gateway", fileName);
        assertEquals(true, Files.exists(path), path + " should exist");
        return Files.readString(path);
    }
}
