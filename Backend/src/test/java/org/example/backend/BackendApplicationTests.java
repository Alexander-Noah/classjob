/*
 * BackendApplicationTests回归测试；测试夹具与生产配置分离。
 * 覆盖场景：applicationEntryPointIsLoadable、applicationEntryPointPreparesLocalDevelopmentSecretsBeforeSpringStarts。
 */
package org.example.backend;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendApplicationTests {
    /**
     * application条目点位IsLoadable的处理逻辑。
     */
    @Test
    void applicationEntryPointIsLoadable() {
        assertDoesNotThrow(/** 处理 assertDoesNotThrow 调用中的回调逻辑。 */ () -> Class.forName("org.example.backend.BackendApplication"));
    }

    /**
     * application条目点位Prepares本地Development秘密配置BeforeSpring开始的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void applicationEntryPointPreparesLocalDevelopmentSecretsBeforeSpringStarts() throws Exception {
        String source = Files.readString(Path.of("src/main/java/org/example/backend/BackendApplication.java"));

        int secretsIndex = source.indexOf("LocalDevelopmentSecretsInitializer.ensure");
        int springRunIndex = source.indexOf("application.run");

        assertTrue(secretsIndex >= 0);
        assertTrue(springRunIndex > secretsIndex);
    }
}
