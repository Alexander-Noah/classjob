/*
 * RequestLoggingFilter配置组件，声明应用启动或框架集成时使用的行为。
 */
package org.example.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final String REQUEST_ID = "requestId";

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
        String requestId = getOrCreateRequestId(request);
        long startTime = System.currentTimeMillis();
        MDC.put(REQUEST_ID, requestId);
        response.setHeader("X-Request-Id", requestId);

        try {
            log.info("request start method={} uri={} query={} ip={} userAgent={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    maskQuery(request.getQueryString()),
                    getClientIp(request),
                    limitLength(request.getHeader("User-Agent"), 160));

            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("request end method={} uri={} status={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);
            MDC.remove(REQUEST_ID);
        }
    }

    /**
     * 获取OrCreate请求标识。
     * @param request 请求参数。
     * @return String 类型的处理结果。
     */
    private String getOrCreateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return requestId;
    }

    /**
     * 获取客户端Ip。
     * @param request 请求参数。
     * @return String 类型的处理结果。
     */
    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * mask查询的处理逻辑。
     * @param query 查询参数。
     * @return String 类型的处理结果。
     */
    private String maskQuery(String query) {
        if (query == null || query.isBlank()) {
            return query;
        }

        StringBuilder masked = new StringBuilder();
        String[] parts = query.split("&");
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) {
                masked.append('&');
            }
            String part = parts[index];
            int equalsIndex = part.indexOf('=');
            String key = equalsIndex >= 0 ? part.substring(0, equalsIndex) : part;
            if (isSensitiveKey(key)) {
                masked.append(key).append("=******");
            } else {
                masked.append(limitLength(part, 120));
            }
        }
        return limitLength(masked.toString(), 500);
    }

    /**
     * 判断是否为Sensitive键。
     * @param key 键参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isSensitiveKey(String key) {
        String normalizedKey = key == null ? "" : key.toLowerCase();
        return normalizedKey.contains("token")
                || normalizedKey.contains("password")
                || normalizedKey.contains("secret")
                || normalizedKey.contains("key")
                || normalizedKey.contains("authorization");
    }

    /**
     * 上限长度的处理逻辑。
     * @param value 值参数。
     * @param maxLength max长度参数。
     * @return String 类型的处理结果。
     */
    private String limitLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
