package org.example.backend.service.impl;

import org.example.backend.entity.AiAssistantUserConfigEntity;
import org.example.backend.mapper.AiAssistantUserConfigMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiProviderCredentialScopeTest {
    private final AiAssistantUserConfigMapper mapper = mock(AiAssistantUserConfigMapper.class);
    private final AiAssistantUserConfigServiceImpl service = new AiAssistantUserConfigServiceImpl(mapper);
    private AiAssistantUserConfigEntity saved() {
        AiAssistantUserConfigEntity saved = new AiAssistantUserConfigEntity();
        saved.setUserId(1); saved.setEnabled(true); saved.setBaseUrl("https://api.deepseek.com");
        saved.setApiKey("fixture-only-key"); saved.setModel("deepseek-flash");
        when(mapper.getByUserId(1)).thenReturn(saved);
        return saved;
    }
    @Test void changingProviderCannotReuseThePreviousProvidersSecret() {
        saved();
        assertThrows(IllegalArgumentException.class, () -> service.saveConfig(1, Map.of("enabled", true, "baseUrl", "https://api.siliconflow.cn/v1", "apiKey", "")));
        verify(mapper, never()).upsertConfig(any());
    }
    @Test void equivalentEndpointAndModelChangesKeepTheExistingKeyWithoutExposingIt() {
        saved();
        Map<String, Object> response = service.saveConfig(1, Map.of("baseUrl", "https://api.deepseek.com/chat/completions/", "model", "different-model"));
        ArgumentCaptor<AiAssistantUserConfigEntity> capture = ArgumentCaptor.forClass(AiAssistantUserConfigEntity.class);
        verify(mapper).upsertConfig(capture.capture());
        assertEquals("fixture-only-key", capture.getValue().getApiKey());
        assertFalse(response.containsKey("apiKey"));
    }
    @Test void switchingWhileDisabledClearsTheOldKey() {
        saved();
        service.saveConfig(1, Map.of("enabled", false, "baseUrl", "https://api.siliconflow.cn/v1"));
        ArgumentCaptor<AiAssistantUserConfigEntity> capture = ArgumentCaptor.forClass(AiAssistantUserConfigEntity.class);
        verify(mapper).upsertConfig(capture.capture());
        assertEquals("", capture.getValue().getApiKey());
    }
    @Test void newProviderAcceptsItsOwnKeyAndRejectsInvalidEndpoints() {
        saved();
        service.saveConfig(1, Map.of("baseUrl", "https://api.siliconflow.cn/v1", "apiKey", "fixture-new-key"));
        ArgumentCaptor<AiAssistantUserConfigEntity> capture = ArgumentCaptor.forClass(AiAssistantUserConfigEntity.class);
        verify(mapper).upsertConfig(capture.capture());
        assertEquals("fixture-new-key", capture.getValue().getApiKey());
        assertThrows(IllegalArgumentException.class, () -> service.saveConfig(1, Map.of("baseUrl", "file:///invalid", "apiKey", "fixture-new-key")));
    }
}
