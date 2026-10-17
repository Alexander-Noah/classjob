/*
 * 业务闭环的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/business-loop/overview；完整路径由类级与方法级映射组合。
 * 主要协作组件：BusinessLoopReportService。
 */
package org.example.backend.controller;

import org.example.backend.result.Result;
import org.example.backend.service.BusinessLoopReportService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class BusinessLoopReportController {
    private final BusinessLoopReportService businessLoopReportService;

    /**
     * 构造 BusinessLoopReportController，初始化当前实例。
     * @param businessLoopReportService businessLoopReport服务参数。
     */
    public BusinessLoopReportController(BusinessLoopReportService businessLoopReportService) {
        this.businessLoopReportService = businessLoopReportService;
    }

    /**
     * 获取BusinessLoop概况。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/business-loop/overview")
    public Result getBusinessLoopOverview() {
        return Result.success("获取业务闭环概览成功", businessLoopReportService.getOverview());
    }
}
