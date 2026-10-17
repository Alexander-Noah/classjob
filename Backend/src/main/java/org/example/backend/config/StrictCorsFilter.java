package org.example.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** 优先处理跨域来源及 OPTIONS 预检；不依赖 Cookie，业务身份通过 Authorization 传递。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StrictCorsFilter extends OncePerRequestFilter {
    private final CorsConfiguration corsConfiguration;

    /**
     * 构造 StrictCorsFilter，初始化当前实例。
     * @param allowedOrigins allowedOrigins参数。
     * @param allowedOriginPatterns allowedOriginPatterns参数。
     */
    public StrictCorsFilter(
            @Value("${smart-lab.security.cors.allowed-origins:}") String allowedOrigins,
            @Value("${smart-lab.security.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*,tauri://localhost,http://tauri.localhost}") String allowedOriginPatterns
    ) {
        this.corsConfiguration = new CorsConfiguration();
        this.corsConfiguration.setAllowedOrigins(splitCsv(allowedOrigins));
        this.corsConfiguration.setAllowedOriginPatterns(splitCsv(allowedOriginPatterns));
        this.corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        this.corsConfiguration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-Request-Id",
                "Accept",
                "Origin"
        ));
        this.corsConfiguration.setExposedHeaders(List.of("X-Request-Id"));
        this.corsConfiguration.setAllowCredentials(false);
        this.corsConfiguration.setMaxAge(1800L);
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
        String origin = request.getHeader(HttpHeaders.ORIGIN);

        if (origin == null || origin.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String allowedOrigin = corsConfiguration.checkOrigin(origin);
        if (allowedOrigin == null) {
            writeForbidden(response);
            return;
        }

        writeCorsHeaders(request, response, allowedOrigin);
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        filterChain.doFilter(request, response);
        writeCorsHeaders(request, response, allowedOrigin);
    }

    /**
     * 写入Cors请求头。
     * @param request 请求参数。
     * @param response 响应参数。
     * @param allowedOrigin allowedOrigin参数。
     */
    private void writeCorsHeaders(HttpServletRequest request, HttpServletResponse response, String allowedOrigin) {
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        response.setHeader(HttpHeaders.VARY, "Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, String.join(",", corsConfiguration.getAllowedMethods()));
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, String.join(",", corsConfiguration.getAllowedHeaders()));
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, String.join(",", corsConfiguration.getExposedHeaders()));
        response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, String.valueOf(corsConfiguration.getMaxAge()));
        String requestedMethod = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        if (requestedMethod != null && !requestedMethod.isBlank()) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, requestedMethod);
        }
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
        response.getWriter().write("{\"code\":403,\"message\":\"CORS origin is not allowed\",\"data\":{}}");
    }

    /**
     * 拆分CSV。
     * @param value 值参数。
     * @return 当前操作得到的记录集合。
     */
    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ item -> !item.isBlank())
                .toList();
    }
}
