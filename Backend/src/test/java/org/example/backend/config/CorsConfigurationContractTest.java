/*
 * CorsConfigurationContractTest回归测试；测试夹具与生产配置分离。
 * 覆盖场景：defaultDevelopmentOriginsAllowPrivateLanQrClients。
 */
package org.example.backend.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigurationContractTest {
    /**
     * 提供默认DevelopmentOriginsAllow私有LanQr客户端。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void defaultDevelopmentOriginsAllowPrivateLanQrClients() throws IOException {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yaml"));

        assertTrue(
                applicationYaml.contains("http://192.168.*:*")
                        && applicationYaml.contains("http://10.*:*")
                        && applicationYaml.contains("http://172.16.*:*")
                        && applicationYaml.contains("http://172.31.*:*"),
                "default CORS patterns must allow browser clients on RFC1918 development networks"
        );
    }
}
