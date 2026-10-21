/*
 * AI 助手配置组件，声明应用启动或框架集成时使用的行为。
 * 主要协作组件：JwtService。
 */
package org.example.backend.config;

import org.example.backend.security.JwtService;
import org.example.backend.service.RolePageAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.example.backend.websocket.AiAssistantWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.Map;

@Configuration
@EnableWebSocket
public class AiAssistantWebSocketConfig implements WebSocketConfigurer {
    private static final int AI_ASSISTANT_MESSAGE_BUFFER_SIZE = 20 * 1024 * 1024;

    private final AiAssistantWebSocketHandler aiAssistantWebSocketHandler;
    private final JwtService jwtService;
    private final String[] allowedOriginPatterns;
    private RolePageAccessService pageAccess;

    @Autowired
    public void configureRolePageAccess(RolePageAccessService pageAccess) {
        this.pageAccess = pageAccess;
    }

    /**
     * 构造 AiAssistantWebSocketConfig，初始化当前实例。
     * @param aiAssistantWebSocketHandler AIAssistantWebSocket处理器参数。
     * @param jwtService JWT服务参数。
     * @param allowedOriginPatterns allowedOriginPatterns参数。
     */
    public AiAssistantWebSocketConfig(
            AiAssistantWebSocketHandler aiAssistantWebSocketHandler,
            JwtService jwtService,
            @Value("${smart-lab.security.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*,tauri://localhost,http://tauri.localhost}") String allowedOriginPatterns
    ) {
        this.aiAssistantWebSocketHandler = aiAssistantWebSocketHandler;
        this.jwtService = jwtService;
        this.allowedOriginPatterns = splitCsv(allowedOriginPatterns);
    }

    /**
     * 注册WebSocket处理器。
     * @param registry registry参数。
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(aiAssistantWebSocketHandler, "/ai-assistant/ws", "/ws/ai-assistant")
                .addInterceptors(new JwtHandshakeInterceptor(jwtService, pageAccess))
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }

    /**
     * webSocketContainer的处理逻辑。
     * @return ServletServerContainerFactoryBean 类型的处理结果。
     */
    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(AI_ASSISTANT_MESSAGE_BUFFER_SIZE);
        container.setMaxBinaryMessageBufferSize(AI_ASSISTANT_MESSAGE_BUFFER_SIZE);
        return container;
    }

    private static class JwtHandshakeInterceptor implements HandshakeInterceptor {
        private final JwtService jwtService;
        private final RolePageAccessService pageAccess;

        /**
         * 构造 JwtHandshakeInterceptor，初始化当前实例。
         * @param jwtService JWT服务参数。
         */
        private JwtHandshakeInterceptor(JwtService jwtService, RolePageAccessService pageAccess) {
            this.jwtService = jwtService;
            this.pageAccess = pageAccess;
        }

        /**
         * 在执行前处理Handshake。
         * @param request 请求参数。
         * @param response 响应参数。
         * @param wsHandler ws处理器参数。
         * @param attributes attributes参数。
         * @return 当前判断的布尔结果。
         */
        @Override
        public boolean beforeHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Map<String, Object> attributes
        ) {
            String token = resolveToken(request);
            if (token.isBlank()) {
                return false;
            }

            try {
                JwtService.JwtClaims claims = jwtService.validateToken(token);
                if (pageAccess != null && !pageAccess.isApiAllowed("GET", "/ai-assistant/ws", claims.roleCode())) return false;
                attributes.put("authUsername", claims.username());
                attributes.put("authRole", claims.roleCode());
                attributes.put("authUserId", claims.userId());
                return true;
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }

        /**
         * 在执行后处理Handshake。
         * @param request 请求参数。
         * @param response 响应参数。
         * @param wsHandler ws处理器参数。
         * @param exception 异常参数。
         */
        @Override
        public void afterHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Exception exception
        ) {
            // No-op.
        }

        /**
         * 解析令牌。
         * @param request 请求参数。
         * @return String 类型的处理结果。
         */
        private String resolveToken(ServerHttpRequest request) {
            String authorization = request.getHeaders().getFirst("Authorization");
            if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return authorization.substring(7).trim();
            }

            String subprotocolToken = resolveTokenFromSubprotocol(request);
            if (!subprotocolToken.isBlank()) {
                return subprotocolToken;
            }

            return "";
        }

        /**
         * 解析令牌来源Subprotocol。
         * @param request 请求参数。
         * @return String 类型的处理结果。
         */
        private String resolveTokenFromSubprotocol(ServerHttpRequest request) {
            String header = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
            if (header == null || header.isBlank()) {
                return "";
            }
            return Arrays.stream(header.split(","))
                    .map(String::trim)
                    .filter(/** 判断当前条目是否符合筛选条件。 入参：protocol。 */ protocol -> protocol.startsWith("smart-lab.jwt."))
                    .map(/** 将当前条目转换为目标结果。 入参：protocol。 */ protocol -> protocol.substring("smart-lab.jwt.".length()).trim())
                    .findFirst()
                    .orElse("");
        }
    }

    /**
     * 拆分CSV。
     * @param value 值参数。
     * @return String[] 类型的处理结果。
     */
    private static String[] splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ item -> !item.isBlank())
                .toArray(String[]::new);
    }
}
