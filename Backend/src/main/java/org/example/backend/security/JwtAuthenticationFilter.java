package org.example.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.backend.security.AuthenticationAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 在角色授权之前校验 Bearer 令牌，并将身份放入请求属性供后续组件使用。
 * 匿名路径应逐项维护；监控采集仅允许通过独立管理端口访问。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String AUTH_USERNAME_ATTRIBUTE = AuthenticationAttributes.USERNAME;
    public static final String AUTH_ROLE_ATTRIBUTE = AuthenticationAttributes.ROLE;
    public static final String AUTH_USER_ID_ATTRIBUTE = AuthenticationAttributes.USER_ID;
    private static final String RETIRED_ROLE_MESSAGE = "角色已停用，请联系系统管理员重新分配角色";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login",
            "/auth/logout",
            "/ai-assistant/ws",
            "/ws/ai-assistant",
            "/error",
            "/favicon.ico",
            "/actuator/health",
            "/activation-codes/verify"
    );

    private static final String[] PUBLIC_PREFIXES = {
            "/public/",
            "/teacher-hosts",
            "/student-clients",
            "/api/teacher-hosts",
            "/api/student-clients"
    };

    private final JwtService jwtService;
    private final boolean enabled;

    @Value("${management.server.port:-1}")
    private int managementPort = -1;

    @Value("${server.port:8080}")
    private int applicationPort = 8080;

    /**
     * 构造 JwtAuthenticationFilter，初始化当前实例。
     * @param jwtService JWT服务参数。
     * @param enabled 已启用参数。
     */
    public JwtAuthenticationFilter(
            JwtService jwtService,
            @Value("${smart-lab.security.jwt.enabled:true}") boolean enabled
    ) {
        this.jwtService = jwtService;
        this.enabled = enabled;
    }

    /**
     * 执行筛选条件Internal。
     * @param request 请求参数。
     * @param response 响应参数。
     * @param filterChain 筛选条件Chain参数。
     * @throws ServletException 当前调用无法继续完成时抛出。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled || shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        if (token.isBlank()) {
            writeUnauthorized(response, "未授权或会话已过期");
            return;
        }

        try {
            JwtService.JwtClaims claims = jwtService.validateToken(token);
            if (RetiredRoles.contains(claims.roleCode())) {
                writeUnauthorized(response, RETIRED_ROLE_MESSAGE);
                return;
            }
            request.setAttribute(AUTH_USERNAME_ATTRIBUTE, claims.username());
            request.setAttribute(AUTH_ROLE_ATTRIBUTE, claims.roleCode());
            request.setAttribute(AUTH_USER_ID_ATTRIBUTE, claims.userId());
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException exception) {
            writeUnauthorized(response, "会话无效，请重新登录");
        }
    }

    /**
     * 判断是否需要Skip。
     * @param request 请求参数。
     * @return 当前判断的布尔结果。
     */
    private boolean shouldSkip(HttpServletRequest request) {
        // 预检请求不携带业务令牌，来源校验由更早执行的 StrictCorsFilter 完成。
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }

        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }

        // 以实际接收请求的本地端口判断监控入口，不能用可由请求方填写的 Host 头放行。
        // 管理端口与业务端口相同时也不放行，避免配置覆盖扩大匿名访问范围。
        if ("/actuator/prometheus".equals(path)
                && request.getLocalPort() == managementPort
                && managementPort != applicationPort) {
            return true;
        }

        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析令牌。
     * @param request 请求参数。
     * @return String 类型的处理结果。
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            return "";
        }
        if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return "";
        }
        return authorization.substring(7).trim();
    }

    /**
     * 写入Unauthorized。
     * @param response 响应参数。
     * @param message 消息参数。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":401,\"message\":\"" + escapeJson(message) + "\",\"data\":{}}");
    }

    /**
     * 转义JSON。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (current < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) current));
                    } else {
                        escaped.append(current);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
