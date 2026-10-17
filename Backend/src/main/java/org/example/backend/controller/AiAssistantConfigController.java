/*
 * AI 助手的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/ai-assistant/user-config、/ai-assistant/user-config、/ai-assistant/user-config/test、/ai-assistant/chat；完整路径由类级与方法级映射组合。
 * 主要协作组件：TongyiAgentService、AiAssistantUserConfigService、AiPlatformChatService。
 */
package org.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.result.Result;
import org.example.backend.security.JwtAuthenticationFilter;
import org.example.backend.service.AiAssistantUserConfigService;
import org.example.backend.service.AiPlatformChatService;
import org.example.backend.service.TongyiAgentService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@CrossOrigin
@RestController
public class AiAssistantConfigController {
    private final TongyiAgentService tongyiAgentService;
    private final AiAssistantUserConfigService userConfigService;
    private final AiPlatformChatService platformChatService;

    /**
     * 构造 AiAssistantConfigController，初始化当前实例。
     * @param tongyiAgentService tongyiAgent服务参数。
     * @param userConfigService 用户配置服务参数。
     * @param platformChatService platformChat服务参数。
     */
    public AiAssistantConfigController(
            TongyiAgentService tongyiAgentService,
            AiAssistantUserConfigService userConfigService,
            AiPlatformChatService platformChatService
    ) {
        this.tongyiAgentService = tongyiAgentService;
        this.userConfigService = userConfigService;
        this.platformChatService = platformChatService;
    }

    /**
     * 获取配置。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping({"/config", "/ai-assistant/config"})
    public Result getConfig(HttpServletRequest request) {
        String host = request.getServerName();
        int port = request.getServerPort();
        String scheme = request.getScheme();
        String wsScheme = "https".equalsIgnoreCase(scheme) ? "wss" : "ws";
        String wsUrl = wsScheme + "://" + host + ":" + port + "/ai-assistant/ws";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("backend_url", scheme + "://" + host + ":" + port);
        data.put("ws_url", wsUrl);
        data.put("ws_proxy_url", wsUrl);
        data.put("agent_provider", "tongyi");
        data.put("tongyi_agent", tongyiAgentService.getConfig());

        return Result.success("获取 AI 助手配置成功", data);
    }

    /**
     * 获取用户配置。
     * @param userId 用户标识参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/ai-assistant/user-config")
    public Result getUserConfig(
            @RequestAttribute(value = JwtAuthenticationFilter.AUTH_USER_ID_ATTRIBUTE, required = false) Integer userId
    ) {
        return Result.success("获取 AI 接口配置成功", userConfigService.getConfig(userId));
    }

    /**
     * 保存用户配置。
     * @param userId 用户标识参数。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PutMapping("/ai-assistant/user-config")
    public Result saveUserConfig(
            @RequestAttribute(value = JwtAuthenticationFilter.AUTH_USER_ID_ATTRIBUTE, required = false) Integer userId,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        return Result.success("保存 AI 接口配置成功", userConfigService.saveConfig(userId, payload));
    }

    /**
     * 验证用户配置。
     * @param userId 用户标识参数。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/ai-assistant/user-config/test")
    public Result testUserConfig(
            @RequestAttribute(value = JwtAuthenticationFilter.AUTH_USER_ID_ATTRIBUTE, required = false) Integer userId,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        return Result.success("AI 接口连接测试成功", userConfigService.testConnection(userId, payload));
    }

    /**
     * chat的处理逻辑。
     * @param userId 用户标识参数。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/ai-assistant/chat")
    public Result chat(
            @RequestAttribute(value = JwtAuthenticationFilter.AUTH_USER_ID_ATTRIBUTE, required = false) Integer userId,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        Map<String, Object> request = payload == null ? Collections.emptyMap() : payload;
        String question = String.valueOf(request.getOrDefault("question", ""));
        java.util.List<Map<String, Object>> history = new java.util.ArrayList<>();
        if (request.get("history") instanceof java.util.List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> message) {
                    history.add(Map.of("role", String.valueOf(message.get("role")),
                            "content", String.valueOf(message.get("content"))));
                }
            }
        }
        return Result.success("AI 回答成功", platformChatService.chat(userId, question, history));
    }
}
