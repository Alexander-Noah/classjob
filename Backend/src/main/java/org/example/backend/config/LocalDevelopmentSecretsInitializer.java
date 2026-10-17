/*
 * LocalDevelopmentSecretsInitializer配置组件，声明应用启动或框架集成时使用的行为。
 */
package org.example.backend.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;

public final class LocalDevelopmentSecretsInitializer {
    private static final String SECRETS_FILE = ".local-dev-secrets.properties";
    private static final String JWT_SECRET = "SMART_LAB_JWT_SECRET";
    private static final String ACADEMIC_SECRET = "ACADEMIC_CREDENTIAL_SECRET";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 构造 LocalDevelopmentSecretsInitializer，初始化当前实例。
     */
    private LocalDevelopmentSecretsInitializer() {
    }

    /**
     * 确保满足当前操作。
     * @param workingDirectory working目录参数。
     * @return 当前判断的布尔结果。
     */
    public static boolean ensure(Path workingDirectory) {
        Path baseDirectory = workingDirectory == null
                ? Path.of("").toAbsolutePath().normalize()
                : workingDirectory.toAbsolutePath().normalize();
        if (!isDevelopmentDirectory(baseDirectory)) {
            return false;
        }

        Path secretsPath = baseDirectory.resolve(SECRETS_FILE);
        Properties secrets = readSecrets(secretsPath);
        boolean changed = false;

        if (isBlank(secrets.getProperty(JWT_SECRET))) {
            secrets.setProperty(JWT_SECRET, newSecret());
            changed = true;
        }
        if (isBlank(secrets.getProperty(ACADEMIC_SECRET))) {
            secrets.setProperty(ACADEMIC_SECRET, newSecret());
            changed = true;
        }

        if (changed) {
            writeSecrets(secretsPath, secrets);
        }
        return true;
    }

    /**
     * 判断是否为Development目录。
     * @param directory 目录参数。
     * @return 当前判断的布尔结果。
     */
    private static boolean isDevelopmentDirectory(Path directory) {
        return Files.isRegularFile(directory.resolve("pom.xml"))
                && containsBackendApplication(directory.resolve("src/main"));
    }

    /**
     * 判断是否包含BackendApplication。
     * @param sourceRoot 来源根目录参数。
     * @return 当前判断的布尔结果。
     */
    private static boolean containsBackendApplication(Path sourceRoot) {
        return Files.isDirectory(sourceRoot)
                && Files.isRegularFile(sourceRoot.resolve("java/org/example/backend/BackendApplication.java"));
    }

    /**
     * 读取秘密配置。
     * @param secretsPath 秘密配置路径参数。
     * @return Properties 类型的处理结果。
     */
    private static Properties readSecrets(Path secretsPath) {
        Properties secrets = new Properties();
        if (!Files.exists(secretsPath)) {
            return secrets;
        }

        try (InputStream inputStream = Files.newInputStream(secretsPath)) {
            secrets.load(inputStream);
            return secrets;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read local development secrets: " + secretsPath, exception);
        }
    }

    /**
     * 写入秘密配置。
     * @param secretsPath 秘密配置路径参数。
     * @param secrets 秘密配置参数。
     */
    private static void writeSecrets(Path secretsPath, Properties secrets) {
        try (OutputStream outputStream = Files.newOutputStream(secretsPath)) {
            secrets.store(outputStream, "Generated local development secrets. Do not commit this file.");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write local development secrets: " + secretsPath, exception);
        }
    }

    /**
     * new秘密配置的处理逻辑。
     * @return String 类型的处理结果。
     */
    private static String newSecret() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 判断是否为空白。
     * @param value 值参数。
     * @return 当前判断的布尔结果。
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
