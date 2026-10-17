/*
 * LocalDevelopmentSecretsInitializerTest回归测试；测试夹具与生产配置分离。
 * 覆盖场景：createsRequiredSecretsWhenRunningFromDevelopmentDirectory、doesNotCreateSecretsOutsideDevelopmentDirectory。
 */
package org.example.backend.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalDevelopmentSecretsInitializerTest {
    /**
     * createsRequired秘密配置WhenRunning来源Development目录的处理逻辑。
     * 涉及：写入文件。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void createsRequiredSecretsWhenRunningFromDevelopmentDirectory() throws Exception {
        Path projectDir = newTestDirectory().resolve("Backend");
        Files.createDirectories(projectDir.resolve("src/main/java/org/example/backend"));
        Files.writeString(
                projectDir.resolve("src/main/java/org/example/backend/BackendApplication.java"),
                "package org.example.backend;"
        );
        Files.writeString(projectDir.resolve("pom.xml"), "<project />");

        assertTrue(ensureLocalDevelopmentSecrets(projectDir));

        Path secretsPath = projectDir.resolve(".local-dev-secrets.properties");
        Properties properties = new Properties();
        try (var inputStream = Files.newInputStream(secretsPath)) {
            properties.load(inputStream);
        }

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(properties.getProperty("SMART_LAB_JWT_SECRET", "").length() >= 32),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(properties.getProperty("ACADEMIC_CREDENTIAL_SECRET", "").length() >= 32)
        );
    }

    /**
     * doesNotCreate秘密配置OutsideDevelopment目录的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void doesNotCreateSecretsOutsideDevelopmentDirectory() throws Exception {
        Path tempDir = newTestDirectory();

        assertFalse(ensureLocalDevelopmentSecrets(tempDir));

        assertFalse(Files.exists(tempDir.resolve(".local-dev-secrets.properties")));
    }

    /**
     * newTest目录的处理逻辑。
     * @return Path 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Path newTestDirectory() throws Exception {
        Path directory = Path.of("target", "local-dev-secrets-test", String.valueOf(System.nanoTime()));
        Files.createDirectories(directory);
        return directory;
    }

    /**
     * 确保满足本地Development秘密配置。
     * @param workingDirectory working目录参数。
     * @return 当前判断的布尔结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private boolean ensureLocalDevelopmentSecrets(Path workingDirectory) throws Exception {
        Method ensure = Class.forName("org.example.backend.config.LocalDevelopmentSecretsInitializer")
                .getMethod("ensure", Path.class);
        return Boolean.TRUE.equals(ensure.invoke(null, workingDirectory));
    }
}
