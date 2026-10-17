/*
 * LocalDevelopmentLaunchScriptTest回归测试；测试夹具与生产配置分离。
 * 覆盖场景：localStartupScriptSuppliesRequiredSecretsWithoutCommittingThem。
 */
package org.example.backend.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalDevelopmentLaunchScriptTest {
    /**
     * 本地StartupScriptSuppliesRequired秘密配置WithoutCommittingThem的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void localStartupScriptSuppliesRequiredSecretsWithoutCommittingThem() throws IOException {
        Path scriptPath = Path.of("start-local.ps1");
        String script = Files.readString(scriptPath);
        String gitignore = Files.readString(Path.of("..", ".gitignore"));
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yaml"));
        String localApplicationYaml = Files.readString(Path.of("src/main/resources/application-local.yaml"));
        String backendApplication = Files.readString(
                Path.of("src/main/java/org/example/backend/BackendApplication.java")
        );

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(script.contains("SMART_LAB_JWT_SECRET")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(script.contains("ACADEMIC_CREDENTIAL_SECRET")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(script.contains("SMART_LAB_DB_PASSWORD")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(script.contains(".local-dev-secrets.properties")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(script.contains("New-LocalSecret")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(script.contains("spring-boot:run")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(script.contains("spring.profiles.active=local")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(gitignore.contains("Backend/.local-dev-secrets.properties")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(applicationYaml.contains("optional:file:.local-dev-secrets.properties")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(applicationYaml.contains("SMART_LAB_DB_PASSWORD:root")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(localApplicationYaml.contains("optional:file:.local-dev-secrets.properties")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(localApplicationYaml.contains("password: ${SMART_LAB_DB_PASSWORD}")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(localApplicationYaml.contains("SMART_LAB_DB_PASSWORD:root")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(backendApplication.contains("setAdditionalProfiles(\"local\")"),
                        "IDE launches from the Backend source directory must activate local configuration")
        );
    }
}
