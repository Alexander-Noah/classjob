/*
 * 密码安全的服务端安全组件；调用方应保持身份认证与权限判断边界。
 */
package org.example.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordService {
    private static final String PREFIX = "pbkdf2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private final SecureRandom secureRandom = new SecureRandom();
    private final int iterations;

    public PasswordService(@Value("${smart-lab.security.password.iterations:120000}") int iterations) {
        this.iterations = Math.max(iterations, 10000);
    }

    /**
     * 为每次保存生成独立随机盐，再使用 PBKDF2 派生密码摘要。
     * @param rawPassword 已由业务入口检查格式的明文密码；禁止写入日志
     * @return pbkdf2$迭代次数$Base64盐$Base64摘要，验证旧密码时使用其中保存的参数
     * @throws IllegalStateException 密码派生失败，包括无效输入导致的底层异常
     */
    public String encode(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword, salt, iterations);

        return String.join("$",
                PREFIX,
                String.valueOf(iterations),
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash)
        );
    }

    /**
     * 验证现有密码，不执行数据库更新。支持历史明文仅用于兼容迁移。
     * @param rawPassword 本次登录输入
     * @param storedPassword 数据库保存的摘要串或尚未迁移的历史密码
     * @return 是否匹配；空输入和损坏的摘要格式均返回 false
     */
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (!isEncoded(storedPassword)) {
            // 旧值验证成功后，由登录业务结合 needsRehash 迁移；不能在这里直接改用户记录。
            return MessageDigest.isEqual(
                    rawPassword.getBytes(StandardCharsets.UTF_8),
                    storedPassword.getBytes(StandardCharsets.UTF_8)
            );
        }

        try {
            String[] parts = storedPassword.split("\\$");
            int storedIterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] actualHash = pbkdf2(rawPassword, salt, storedIterations);

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** 返回是否需要升级格式/迭代次数；必须在密码匹配成功后使用明文重新 encode 并保存。 */
    public boolean needsRehash(String storedPassword) {
        if (!isEncoded(storedPassword)) {
            return true;
        }

        try {
            String[] parts = storedPassword.split("\\$");
            return Integer.parseInt(parts[1]) < iterations;
        } catch (RuntimeException exception) {
            return true;
        }
    }

    /** 只识别前缀和四段结构；不保证 Base64、迭代次数或摘要内容合法。 */
    public boolean isEncoded(String password) {
        return password != null && password.startsWith(PREFIX + "$") && password.split("\\$").length == 4;
    }

    private byte[] pbkdf2(String rawPassword, byte[] salt, int iterationCount) {
        try {
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterationCount, HASH_BITS);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("密码哈希转换失败", exception);
        }
    }
}
