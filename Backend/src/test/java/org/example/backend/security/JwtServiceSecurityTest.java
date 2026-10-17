/*
 * JWT 登录令牌回归测试；测试夹具与生产配置分离。
 * 覆盖场景：rejectsBlankOrDevelopmentJwtSecret、acceptsStrongExplicitJwtSecret。
 */
package org.example.backend.security;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceSecurityTest {
    /**
     * rejects空白OrDevelopmentJWT秘密配置的处理逻辑。
     */
    @Test
    void rejectsBlankOrDevelopmentJwtSecret() {
        assertThrows(IllegalStateException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> newJwtService("", 7200, "smart-lab"));
        assertThrows(IllegalStateException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> newJwtService(
                "smart-lab-dev-jwt-secret-change-in-" + "production-2026",
                7200,
                "smart-lab"
        ));
    }

    /**
     * acceptsStrongExplicitJWT秘密配置的处理逻辑。
     */
    @Test
    void acceptsStrongExplicitJwtSecret() {
        assertDoesNotThrow(/** 处理 assertDoesNotThrow 调用中的回调逻辑。 */ () -> newJwtService("test-only-strong-jwt-secret-with-40-plus-chars", 7200, "smart-lab"));
    }

    /**
     * newJWT服务的处理逻辑。
     * @param secret 秘密配置参数。
     * @param expirationSeconds expirationSeconds参数。
     * @param issuer issuer参数。
     * @return Object 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Object newJwtService(String secret, long expirationSeconds, String issuer) throws Exception {
        try {
            return Class.forName("org.example.backend.security.JwtService")
                    .getConstructor(String.class, long.class, String.class)
                    .newInstance(secret, expirationSeconds, issuer);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }
}
