/*
 * 工作台统计的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/dashboard/overview；完整路径由类级与方法级映射组合。
 * 主要协作组件：DashboardService。
 */
package org.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.result.Result;
import org.example.backend.security.JwtAuthenticationFilter;
import org.example.backend.service.DashboardService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class DashboardController {
    private final DashboardService dashboardService;

    /**
     * 构造 DashboardController，初始化当前实例。
     * @param dashboardService dashboard服务参数。
     */
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 获取概况。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/dashboard/overview")
    public Result getOverview(HttpServletRequest request) {
        return Result.success("获取工作台概览成功", dashboardService.getOverview(resolveScopedManagerId(request)));
    }

    /**
     * 解析ScopedManager标识。
     * @param request 请求参数。
     * @return Integer 类型的处理结果。
     */
    private Integer resolveScopedManagerId(HttpServletRequest request) {
        Object role = request.getAttribute(JwtAuthenticationFilter.AUTH_ROLE_ATTRIBUTE);
        if ("systemAdmin".equals(String.valueOf(role))) {
            return null;
        }

        Object rawUserId = request.getAttribute(JwtAuthenticationFilter.AUTH_USER_ID_ATTRIBUTE);
        if (rawUserId == null) {
            return null;
        }

        try {
            return Integer.valueOf(String.valueOf(rawUserId));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
