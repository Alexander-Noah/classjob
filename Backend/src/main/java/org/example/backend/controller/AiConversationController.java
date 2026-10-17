/*
 * AI 会话历史的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/ai-assistant/conversations、/ai-assistant/conversations、/ai-assistant/conversations/{conversationId}；完整路径由类级与方法级映射组合。
 * 主要协作组件：AiConversationService。
 */
package org.example.backend.controller;

import lombok.Data;
import org.example.backend.result.Result;
import org.example.backend.security.JwtAuthenticationFilter;
import org.example.backend.service.AiConversationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class AiConversationController {
    private final AiConversationService aiConversationService;

    /**
     * 构造 AiConversationController，初始化当前实例。
     * @param aiConversationService AI会话服务参数。
     */
    public AiConversationController(AiConversationService aiConversationService) {
        this.aiConversationService = aiConversationService;
    }

    /**
     * 列出会话。
     * @param userId 用户标识参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/ai-assistant/conversations")
    public Result listConversations(
            @RequestAttribute(value = JwtAuthenticationFilter.AUTH_USER_ID_ATTRIBUTE, required = false) Integer userId
    ) {
        return Result.success("获取 AI 会话记录成功", aiConversationService.listConversations(userId));
    }

    /**
     * 保存会话。
     * @param userId 用户标识参数。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @PutMapping("/ai-assistant/conversations")
    public Result saveConversations(
            @RequestAttribute(value = JwtAuthenticationFilter.AUTH_USER_ID_ATTRIBUTE, required = false) Integer userId,
            @RequestBody(required = false) ConversationBatchRequest request
    ) {
        List<Map<String, Object>> conversations = request == null || request.getConversations() == null
                ? Collections.emptyList()
                : request.getConversations();
        return Result.success("保存 AI 会话记录成功", aiConversationService.saveConversations(userId, conversations));
    }

    /**
     * 删除会话。
     * @param userId 用户标识参数。
     * @param conversationId 会话标识参数。
     * @return Result 类型的处理结果。
     */
    @DeleteMapping("/ai-assistant/conversations/{conversationId}")
    public Result deleteConversation(
            @RequestAttribute(value = JwtAuthenticationFilter.AUTH_USER_ID_ATTRIBUTE, required = false) Integer userId,
            @PathVariable("conversationId") String conversationId
    ) {
        aiConversationService.deleteConversation(userId, conversationId);
        return Result.success("删除 AI 会话记录成功", null);
    }

    @Data
    public static class ConversationBatchRequest {
        private List<Map<String, Object>> conversations;
    }
}
