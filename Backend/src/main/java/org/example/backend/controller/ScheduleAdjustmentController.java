/*
 * 调课申请的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/schedule-adjustments、/schedule-adjustments、/schedule-adjustments/{id}、/schedule-adjustments/{id}/approve、/schedule-adjustments/{id}/complete；完整路径由类级与方法级映射组合。
 * 主要协作组件：ScheduleAdjustmentService。
 */
package org.example.backend.controller;

import org.example.backend.result.Result;
import org.example.backend.entity.ScheduleAdjustmentEntity;
import org.example.backend.service.ScheduleAdjustmentService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
public class ScheduleAdjustmentController {
    private final ScheduleAdjustmentService scheduleAdjustmentService;

    /**
     * 构造 ScheduleAdjustmentController，初始化当前实例。
     * @param scheduleAdjustmentService 课表Adjustment服务参数。
     */
    public ScheduleAdjustmentController(ScheduleAdjustmentService scheduleAdjustmentService) {
        this.scheduleAdjustmentService = scheduleAdjustmentService;
    }

    /**
     * 列出Adjustments。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/schedule-adjustments")
    public Result listAdjustments() {
        return Result.success("获取调课申请列表成功", scheduleAdjustmentService.listAdjustments());
    }

    /**
     * 创建Adjustment。
     * @param adjustment adjustment参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/schedule-adjustments")
    public Result createAdjustment(@RequestBody ScheduleAdjustmentEntity adjustment) {
        return Result.success("新增调课申请成功", scheduleAdjustmentService.createAdjustment(adjustment));
    }

    /**
     * 更新Adjustment。
     * @param id 标识参数。
     * @param adjustment adjustment参数。
     * @return Result 类型的处理结果。
     */
    @PutMapping("/schedule-adjustments/{id}")
    public Result updateAdjustment(@PathVariable Long id, @RequestBody ScheduleAdjustmentEntity adjustment) {
        return Result.success("更新调课申请成功", scheduleAdjustmentService.updateAdjustment(id, adjustment));
    }

    /**
     * 批准Adjustment。
     * @param id 标识参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/schedule-adjustments/{id}/approve")
    public Result approveAdjustment(@PathVariable Long id) {
        return Result.success("通过调课申请成功", scheduleAdjustmentService.approveAdjustment(id));
    }

    /**
     * 完成Adjustment。
     * @param id 标识参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/schedule-adjustments/{id}/complete")
    public Result completeAdjustment(@PathVariable Long id) {
        return Result.success("完成调课同步成功", scheduleAdjustmentService.completeAdjustment(id));
    }
}
