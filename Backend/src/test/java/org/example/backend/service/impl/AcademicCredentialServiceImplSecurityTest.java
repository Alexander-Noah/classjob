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
    @Test
    void rejectsBlankAcademicCredentialSecret() {
        assertThrows(IllegalStateException.class, () -> newAcademicCredentialService("data/test-academic-credentials.properties", ""));
    }

    @Test
    void acceptsStrongAcademicCredentialSecret() {
        assertDoesNotThrow(() -> newAcademicCredentialService(
                "data/test-academic-credentials.properties",
                "test-only-strong-academic-secret-with-40-plus-chars"
        ));
    }

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
