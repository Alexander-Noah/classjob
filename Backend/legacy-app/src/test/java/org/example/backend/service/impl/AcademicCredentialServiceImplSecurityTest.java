package org.example.backend.service.impl;

import org.example.backend.entity.AcademicScheduleImportRequest;
import org.example.backend.mapper.ClassTimetableMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AcademicCredentialServiceImplSecurityTest {
    private static final String STRONG_SECRET = "test-only-strong-academic-secret-with-40-plus-chars";

    @Test
    void rejectsBlankAcademicCredentialSecret() {
        assertThrows(IllegalStateException.class,
                () -> new AcademicCredentialServiceImpl(mock(ClassTimetableMapper.class), ""));
    }

    @Test
    void acceptsStrongAcademicCredentialSecret() {
        assertDoesNotThrow(() -> new AcademicCredentialServiceImpl(mock(ClassTimetableMapper.class), STRONG_SECRET));
    }

    @Test
    void persistsOnlyEncryptedCredentialValues() {
        ClassTimetableMapper mapper = mock(ClassTimetableMapper.class);
        AcademicCredentialServiceImpl service = new AcademicCredentialServiceImpl(mapper, STRONG_SECRET);
        AcademicScheduleImportRequest request = new AcademicScheduleImportRequest();
        request.setCredentialKey("teacher-import");
        request.setUsername("teacher-user");
        request.setPassword("teacher-password");

        service.saveCredential(request);

        ArgumentCaptor<String> usernameCipher = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> passwordCipher = ArgumentCaptor.forClass(String.class);
        verify(mapper).upsertCrawlerCredential(eq("teacher-import"), usernameCipher.capture(), passwordCipher.capture());
        assertNotEquals("teacher-user", usernameCipher.getValue());
        assertNotEquals("teacher-password", passwordCipher.getValue());
    }
}
