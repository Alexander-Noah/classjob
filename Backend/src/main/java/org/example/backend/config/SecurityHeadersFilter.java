/*
 * 安全策略配置组件，声明应用启动或框架集成时使用的行为。
 */
package org.example.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class SecurityHeadersFilter extends OncePerRequestFilter {
    private final boolean enabled;

    /**
     * 构造 SecurityHeadersFilter，初始化当前实例。
     * @param enabled 已启用参数。
     */
    public SecurityHeadersFilter(@Value("${smart-lab.security.headers.enabled:true}") boolean enabled) {
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
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        writeHeaders(request, response);
        filterChain.doFilter(request, response);
        writeHeaders(request, response);
    }

    /**
     * 写入请求头。
     * @param request 请求参数。
     * @param response 响应参数。
     */
    private void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "camera=(), geolocation=(), microphone=(self)");
        response.setHeader("Content-Security-Policy", "frame-ancestors 'self'");

        String uri = request.getRequestURI();
        if (uri != null && (uri.startsWith("/auth/") || uri.startsWith("/admin/") || uri.startsWith("/users"))) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
        }
    }
}
