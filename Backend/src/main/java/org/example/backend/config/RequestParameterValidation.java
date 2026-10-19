package org.example.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import java.util.Map;

/** Query limits and numeric route IDs are checked before controller/service execution. */
@Configuration
public class RequestParameterValidation implements WebMvcConfigurer {
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (!(handler instanceof HandlerMethod method)
                        || !method.getBeanType().getPackageName().startsWith("org.example.backend.controller")) return true;
                if (request.getParameterMap().size() > 100) throw new IllegalArgumentException("查询参数过多");
                request.getParameterMap().forEach((key, values) -> {
                    if (key.length() > 100 || values.length > 100) throw new IllegalArgumentException("查询参数过多或字段名过长");
                    for (String value : values) {
                        if (value.length() > 4096 || value.codePoints().anyMatch(Character::isISOControl))
                            throw new IllegalArgumentException("查询参数过长或包含非法控制字符");
                        if (java.util.Set.of("page", "pageNum", "pageSize", "size").contains(key) && !value.isBlank()) {
                            try {
                                int number = Integer.parseInt(value);
                                boolean pageNumber = key.equals("page") || key.equals("pageNum");
                                if (number < 1 || (!pageNumber && number > 1000) || (pageNumber && number > 1000000))
                                    throw new NumberFormatException();
                            } catch (NumberFormatException e) { throw new IllegalArgumentException("分页参数无效：页码为 1 至 1000000，每页条数为 1 至 1000"); }
                        }
                    }
                });
                Object raw = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                if (raw instanceof Map<?,?> paths) {
                    for (var parameter : method.getMethodParameters()) {
                        var annotation = parameter.getParameterAnnotation(org.springframework.web.bind.annotation.PathVariable.class);
                        if (annotation == null) continue;
                        Class<?> type = parameter.getParameterType();
                        if (type != Long.class && type != long.class && type != Integer.class && type != int.class) continue;
                        String name = !annotation.value().isBlank() ? annotation.value() : !annotation.name().isBlank() ? annotation.name() : parameter.getParameterName();
                        String value = String.valueOf(paths.get(name));
                        try {
                            long id = Long.parseLong(value);
                            if (id <= 0 || ((type == Integer.class || type == int.class) && id > Integer.MAX_VALUE)) throw new NumberFormatException();
                        } catch (NumberFormatException e) { throw new IllegalArgumentException("资源编号必须为范围内的正整数"); }
                    }
                }
                return true;
            }
        });
    }
}
