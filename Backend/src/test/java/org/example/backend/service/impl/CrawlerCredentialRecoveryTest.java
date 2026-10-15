package org.example.backend.service.impl;

import org.example.backend.mapper.ClassTimetableMapper;
import org.example.backend.service.CrawlerCredentialException;
import org.example.backend.service.TeachingScheduleQuery;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CrawlerCredentialRecoveryTest {
    private static final String KEY_A = "test-only-credential-key-aaaaaaaaaaaaaaaaaaaa";
    private static final String KEY_B = "test-only-credential-key-bbbbbbbbbbbbbbbbbbbb";

    private ClassTimetableServiceImpl service(ClassTimetableMapper mapper, String key) {
        return new ClassTimetableServiceImpl(mapper, mock(TeachingScheduleQuery.class),
                new MockEnvironment(), mock(ThreadPoolTaskScheduler.class), key);
    }

    private ClassTimetableMapper storedMapper(AtomicReference<Map<String, Object>> row) {
        ClassTimetableMapper mapper = mock(ClassTimetableMapper.class);
        when(mapper.getCrawlerCredential(anyString())).thenAnswer(invocation -> row.get());
        when(mapper.upsertCrawlerCredential(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            row.set(new LinkedHashMap<>(Map.of("usernameCipher", invocation.getArgument(1),
                    "passwordCipher", invocation.getArgument(2), "updatedAt", "2026-10-15 12:00:00")));
            return 1;
        });
        return mapper;
    }

    @Test
    void keyMismatchIsARecoverableStateAndNeverDeletesStoredCredentials() {
        var row = new AtomicReference<Map<String,Object>>();
        var mapper = storedMapper(row);
        service(mapper, KEY_A).saveCrawlerCredential(Map.of("username", "fixture-teacher", "password", "fixture-password"));
        var saved = Map.copyOf(row.get());
        clearInvocations(mapper);
        var result = service(mapper, KEY_B).getCrawlerCredential();
        assertTrue(result.getConfigured());
        assertFalse(result.getUsable());
        assertEquals("needs_update", result.getStatus());
        assertEquals(CrawlerCredentialException.USER_MESSAGE, result.getMessage());
        assertNull(result.getUsernameMasked());
        assertEquals(saved, row.get());
        verify(mapper).getCrawlerCredential(anyString());
        verifyNoMoreInteractions(mapper);
        assertThrows(CrawlerCredentialException.class, () -> service(mapper, KEY_B).triggerCrawler());
    }

    @Test
    void resavingWithTheCurrentKeyRestoresUsabilityAndOnlyReturnsMaskedIdentity() {
        var row = new AtomicReference<Map<String,Object>>();
        var mapper = storedMapper(row);
        service(mapper, KEY_A).saveCrawlerCredential(Map.of("username", "fixture-teacher", "password", "fixture-password"));
        var result = service(mapper, KEY_B).saveCrawlerCredential(Map.of("username", "fixture-teacher", "password", "fixture-password"));
        assertTrue(result.getConfigured());
        assertTrue(result.getUsable());
        assertEquals("ready", result.getStatus());
        assertNull(result.getMessage());
        assertNotEquals("fixture-teacher", result.getUsernameMasked());
        assertFalse(result.toString().contains("fixture-password"));
    }

    @Test
    void corruptedPasswordIsDetectedEvenWhenTheUsernameCanBeRead() {
        var row = new AtomicReference<Map<String,Object>>();
        var mapper = storedMapper(row);
        var service = service(mapper, KEY_A);
        service.saveCrawlerCredential(Map.of("username", "fixture-teacher", "password", "fixture-password"));
        row.get().put("passwordCipher", "invalid-encoded-value");
        assertEquals("needs_update", service.getCrawlerCredential().getStatus());
    }

    @Test
    void missingCredentialsRemainDistinctFromUnreadableCredentials() {
        var row = new AtomicReference<Map<String,Object>>();
        var result = service(storedMapper(row), KEY_A).getCrawlerCredential();
        assertFalse(result.getConfigured());
        assertFalse(result.getUsable());
        assertEquals("not_configured", result.getStatus());
    }
}
