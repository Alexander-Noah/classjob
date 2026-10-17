/*
 * RedisHealthConfigurationTest回归测试；测试夹具与生产配置分离。
 * 覆盖场景：redisHealthCheckIsOptInForLocalDevelopment。
 */
package org.example.backend.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisHealthConfigurationTest {
    /**
     * redisHealthCheckIsOpt进入For本地Development的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void redisHealthCheckIsOptInForLocalDevelopment() throws IOException {
        String applicationProperties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertTrue(applicationProperties.contains(
                "management.health.redis.enabled=${SMART_LAB_REDIS_HEALTH_ENABLED:false}"
        ));
    }
}
