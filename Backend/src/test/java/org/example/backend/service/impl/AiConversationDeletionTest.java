/*
 * AI 会话历史回归测试；测试夹具与生产配置分离。
 * 覆盖场景：deletionIsScopedToTheAuthenticatedUser、invalidUserOrConversationCannotReachTheDatabase。
 * 主要协作组件：AiConversationMapper。
 */
package org.example.backend.service.impl;

import org.example.backend.mapper.AiConversationMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AiConversationDeletionTest {
    private final AiConversationMapper mapper = mock(AiConversationMapper.class);
    private final AiConversationServiceImpl service = new AiConversationServiceImpl(mapper);

    /**
     * deletionIsScoped目标The已认证状态用户的处理逻辑。
     * 涉及：写入数据库。
     */
    @Test
    void deletionIsScopedToTheAuthenticatedUser() {
        service.deleteConversation(7, "conversation-example");
        verify(mapper).deleteByUserAndConversationId(7, "conversation-example");
        verifyNoMoreInteractions(mapper);
    }

    /**
     * invalid用户Or会话CannotReachTheDatabase的处理逻辑。
     */
    @Test
    void invalidUserOrConversationCannotReachTheDatabase() {
        assertThrows(IllegalArgumentException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> service.deleteConversation(null, "example"));
        assertThrows(IllegalArgumentException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> service.deleteConversation(0, "example"));
        assertThrows(IllegalArgumentException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> service.deleteConversation(7, " "));
        assertThrows(IllegalArgumentException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> service.deleteConversation(7, null));
        verifyNoInteractions(mapper);
    }
}
