/*
 * AiProviderCredentialScopeTest回归测试；测试夹具与生产配置分离。
 * 覆盖场景：changingProviderCannotReuseThePreviousProvidersSecret、equivalentEndpointAndModelChangesKeepTheExistingKeyWithoutExposingIt、switchingWhileDisabledClearsTheOldKey、newProviderAcceptsItsOwnKeyAndRejectsInvalidEndpoints。
 * 主要协作组件：AiAssistantUserConfigMapper。
 */
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
    /**
     * saved的处理逻辑。
     * @return AiAssistantUserConfigEntity 类型的处理结果。
     */
    private AiAssistantUserConfigEntity saved() {
        AiAssistantUserConfigEntity saved = new AiAssistantUserConfigEntity();
        saved.setUserId(1); saved.setEnabled(true); saved.setBaseUrl("https://api.deepseek.com");
        saved.setApiKey("fixture-only-key"); saved.setModel("deepseek-flash");
        when(mapper.getByUserId(1)).thenReturn(saved);
        return saved;
    }
    /**
     * changing服务商CannotReuseThePrevious服务商秘密配置的处理逻辑。
     */
    @Test void changingProviderCannotReuseThePreviousProvidersSecret() {
        saved();
        assertThrows(IllegalArgumentException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> service.saveConfig(1, Map.of("enabled", true, "baseUrl", "https://api.siliconflow.cn/v1", "apiKey", "")));
        verify(mapper, never()).upsertConfig(any());
    }
    /**
     * equivalent端点And模型ChangesKeepTheExisting键WithoutExposingIt的处理逻辑。
     */
    @Test void equivalentEndpointAndModelChangesKeepTheExistingKeyWithoutExposingIt() {
        saved();
        Map<String, Object> response = service.saveConfig(1, Map.of("baseUrl", "https://api.deepseek.com/chat/completions/", "model", "different-model"));
        ArgumentCaptor<AiAssistantUserConfigEntity> capture = ArgumentCaptor.forClass(AiAssistantUserConfigEntity.class);
        verify(mapper).upsertConfig(capture.capture());
        assertEquals("fixture-only-key", capture.getValue().getApiKey());
        assertFalse(response.containsKey("apiKey"));
    }
    /**
     * switchingWhile已禁用ClearsTheOld键的处理逻辑。
     */
    @Test void switchingWhileDisabledClearsTheOldKey() {
        saved();
        service.saveConfig(1, Map.of("enabled", false, "baseUrl", "https://api.siliconflow.cn/v1"));
        ArgumentCaptor<AiAssistantUserConfigEntity> capture = ArgumentCaptor.forClass(AiAssistantUserConfigEntity.class);
        verify(mapper).upsertConfig(capture.capture());
        assertEquals("", capture.getValue().getApiKey());
    }
    /**
     * new服务商AcceptsItsOwn键AndRejectsInvalid端点的处理逻辑。
     */
    @Test void newProviderAcceptsItsOwnKeyAndRejectsInvalidEndpoints() {
        saved();
        service.saveConfig(1, Map.of("baseUrl", "https://api.siliconflow.cn/v1", "apiKey", "fixture-new-key"));
        ArgumentCaptor<AiAssistantUserConfigEntity> capture = ArgumentCaptor.forClass(AiAssistantUserConfigEntity.class);
        verify(mapper).upsertConfig(capture.capture());
        assertEquals("fixture-new-key", capture.getValue().getApiKey());
        assertThrows(IllegalArgumentException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> service.saveConfig(1, Map.of("baseUrl", "file:///invalid", "apiKey", "fixture-new-key")));
    }
}
