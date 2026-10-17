/*
 * 教务凭据回归测试；测试夹具与生产配置分离。
 * 覆盖场景：rejectsBlankAcademicCredentialSecret、acceptsStrongAcademicCredentialSecret。
 */
package org.example.backend.service.impl;

import org.example.backend.mapper.ClassTimetableMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AcademicCredentialServiceImplSecurityTest {
    /**
     * rejects空白教务凭据秘密配置的处理逻辑。
     */
    @Test
    void rejectsBlankAcademicCredentialSecret() {
        assertThrows(IllegalStateException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> newAcademicCredentialService("data/test-academic-credentials.properties", ""));
    }

    /**
     * acceptsStrong教务凭据秘密配置的处理逻辑。
     */
    @Test
    void acceptsStrongAcademicCredentialSecret() {
        assertDoesNotThrow(/** 处理 assertDoesNotThrow 调用中的回调逻辑。 */ () -> newAcademicCredentialService(
                "data/test-academic-credentials.properties",
                "test-only-strong-academic-secret-with-40-plus-chars"
        ));
    }

    /**
     * new教务凭据服务的处理逻辑。
     * @param storePath store路径参数。
     * @param secret 秘密配置参数。
     * @return Object 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Object newAcademicCredentialService(String storePath, String secret) throws Exception {
        try {
            return Class.forName("org.example.backend.service.impl.AcademicCredentialServiceImpl")
                    .getConstructor(ClassTimetableMapper.class, String.class)
                    .newInstance(mock(ClassTimetableMapper.class), secret);
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
