/*
 * 审批会签的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/approval-countersigns、/approval-countersigns、/approval-countersigns/{id:\\d+}/complete、/approval-countersigns/{id:\\d+}/cancel；完整路径由类级与方法级映射组合。
 * 主要协作组件：ApprovalCountersignService。
 */
package org.example.backend.controller;

import org.example.backend.entity.ApprovalCountersignEntity;
import org.example.backend.result.Result;
import org.example.backend.service.ApprovalCountersignService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApprovalCountersignController {
    private final ApprovalCountersignService countersignService;

    /**
     * 构造 ApprovalCountersignController，初始化当前实例。
     * @param countersignService countersign服务参数。
     */
    public ApprovalCountersignController(ApprovalCountersignService countersignService) {
        this.countersignService = countersignService;
    }

    /**
     * 列出Countersigns。
     * @param businessType business类型参数。
     * @param businessId business标识参数。
     * @param assigneeName assignee名称参数。
     * @param status 状态参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/approval-countersigns")
    public Result listCountersigns(
            @RequestParam(value = "businessType", required = false) String businessType,
            @RequestParam(value = "businessId", required = false) String businessId,
            @RequestParam(value = "assigneeName", required = false) String assigneeName,
            @RequestParam(value = "status", required = false) String status
    ) {
        return Result.success("获取加签列表成功", countersignService.listCountersigns(businessType, businessId, assigneeName, status));
    }

    /**
     * 创建Countersign。
     * @param countersign countersign参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/approval-countersigns")
    public Result createCountersign(@RequestBody ApprovalCountersignEntity countersign) {
        return Result.success("发起加签成功", countersignService.createCountersign(countersign));
    }

    /**
     * 完成Countersign。
     * @param id 标识参数。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/approval-countersigns/{id:\\d+}/complete")
    public Result completeCountersign(@PathVariable Long id, @RequestBody(required = false) ApprovalCountersignEntity payload) {
        return Result.success("处理加签成功", countersignService.completeCountersign(id, payload));
    }

    /**
     * 取消Countersign。
     * @param id 标识参数。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/approval-countersigns/{id:\\d+}/cancel")
    public Result cancelCountersign(@PathVariable Long id, @RequestBody(required = false) ApprovalCountersignEntity payload) {
        return Result.success("撤销加签成功", countersignService.cancelCountersign(id, payload));
    }
}
