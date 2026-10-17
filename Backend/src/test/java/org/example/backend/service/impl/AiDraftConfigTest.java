/*
 * AiDraftConfigTest回归测试；测试夹具与生产配置分离。
 * 覆盖场景：testsUnsavedDraftWithoutPersistingIt、draftCannotBorrowAKeyFromAnotherProvider。
 */
package org.example.backend.service.impl;

import com.sun.net.httpserver.HttpServer;
import org.example.backend.entity.AiAssistantUserConfigEntity;
import org.example.backend.mapper.AiAssistantUserConfigMapper;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiDraftConfigTest {
    /**
     * testsUnsavedDraftWithoutPersistingIt的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void testsUnsavedDraftWithoutPersistingIt() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        server.createContext("/chat/completions", /** 处理 createContext 调用中的回调逻辑。 入参：exchange。 */ exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            AiAssistantUserConfigMapper mapper = mock(AiAssistantUserConfigMapper.class);
            AiAssistantUserConfigServiceImpl service = new AiAssistantUserConfigServiceImpl(mapper);
            Map<String, Object> result = service.testConnection(1, Map.of(
                    "enabled", true, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort(),
                    "apiKey", "fixture-draft-key", "model", "draft-model"));
            assertEquals(true, result.get("connected"));
            assertEquals("Bearer fixture-draft-key", authorization.get());
            assertTrue(body.get().contains("draft-model"));
            assertFalse(result.containsKey("apiKey"));
            verify(mapper, never()).upsertConfig(any());
        } finally {
            server.stop(0);
        }
    }

    /**
     * draftCannotBorrowA键来源Another服务商的处理逻辑。
     */
    @Test
    void draftCannotBorrowAKeyFromAnotherProvider() {
        AiAssistantUserConfigMapper mapper = mock(AiAssistantUserConfigMapper.class);
        AiAssistantUserConfigEntity saved = new AiAssistantUserConfigEntity();
        saved.setBaseUrl("https://api.deepseek.com");
        saved.setApiKey("fixture-saved-key");
        when(mapper.getByUserId(1)).thenReturn(saved);
        AiAssistantUserConfigServiceImpl service = new AiAssistantUserConfigServiceImpl(mapper);
        assertThrows(IllegalArgumentException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> service.testConnection(1, Map.of(
                "enabled", true, "baseUrl", "https://api.siliconflow.cn/v1", "model", "draft-model")));
        verify(mapper, never()).upsertConfig(any());
    }
}
