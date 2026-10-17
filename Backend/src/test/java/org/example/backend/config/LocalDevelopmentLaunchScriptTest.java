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
                () -> assertTrue(script.contains("SMART_LAB_JWT_SECRET")),
                () -> assertTrue(script.contains("ACADEMIC_CREDENTIAL_SECRET")),
                () -> assertTrue(script.contains("SMART_LAB_DB_PASSWORD")),
                () -> assertTrue(script.contains(".local-dev-secrets.properties")),
                () -> assertTrue(script.contains("New-LocalSecret")),
                () -> assertTrue(script.contains("spring-boot:run")),
                () -> assertTrue(script.contains("spring.profiles.active=local")),
                () -> assertTrue(gitignore.contains("Backend/.local-dev-secrets.properties")),
                () -> assertFalse(applicationYaml.contains("optional:file:.local-dev-secrets.properties")),
                () -> assertFalse(applicationYaml.contains("SMART_LAB_DB_PASSWORD:root")),
                () -> assertTrue(localApplicationYaml.contains("optional:file:.local-dev-secrets.properties")),
                () -> assertTrue(localApplicationYaml.contains("password: ${SMART_LAB_DB_PASSWORD}")),
                () -> assertFalse(localApplicationYaml.contains("SMART_LAB_DB_PASSWORD:root")),
                () -> assertTrue(backendApplication.contains("setAdditionalProfiles(\"local\")"),
                        "IDE launches from the Backend source directory must activate local configuration")
        );
    }
}
