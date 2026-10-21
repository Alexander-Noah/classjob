package org.example.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.example.backend.service.RolePageAccessService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 读取认证过滤器写入的角色并执行接口授权；无身份请求仍由认证层负责拦截。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 25)
public class RoleAuthorizationFilter extends OncePerRequestFilter {
    private final SecurityAccessPolicy accessPolicy;
    private RolePageAccessService pageAccess;

    @Autowired
    public RoleAuthorizationFilter(ObjectProvider<RolePageAccessService> policies) {
        this(new SecurityAccessPolicy());
        this.pageAccess = policies.getIfAvailable();
    }

    /**
     * 构造 RoleAuthorizationFilter，初始化当前实例。
     */
    public RoleAuthorizationFilter() {
        this(new SecurityAccessPolicy());
    }

    /**
     * 构造 RoleAuthorizationFilter，初始化当前实例。
     * @param accessPolicy accessPolicy参数。
     */
    RoleAuthorizationFilter(SecurityAccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
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
        String roleCode = stringAttribute(request, JwtAuthenticationFilter.AUTH_ROLE_ATTRIBUTE);
        if (roleCode.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }

        if (accessPolicy.isAllowed(request.getMethod(), path, roleCode)
                && (pageAccess == null || pageAccess.isApiAllowed(request.getMethod(), path, roleCode))) {
            filterChain.doFilter(request, response);
            return;
        }

        writeForbidden(response);
    }

    /**
     * 字符串Attribute的处理逻辑。
     * @param request 请求参数。
     * @param name 名称参数。
     * @return String 类型的处理结果。
     */
    private String stringAttribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 写入Forbidden。
     * @param response 响应参数。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":403,\"message\":\"Forbidden\",\"data\":{}}");
    }
}
