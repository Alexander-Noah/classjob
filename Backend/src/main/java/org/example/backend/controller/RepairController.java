/*
 * 报修的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/repairs、/repairs、/repairs/{id}、/repairs/{id}、/repairs/batch；完整路径由类级与方法级映射组合。
 * 主要协作组件：RepairService。
 */
package org.example.backend.controller;

import lombok.Data;
import org.example.backend.result.Result;
import org.example.backend.entity.RepairEntity;
import org.example.backend.service.RepairService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@CrossOrigin
@RestController
public class RepairController {
    private static final String RETIRED_WORKFLOW_MESSAGE = "故障工单流程已停用，请使用维护记录功能";
    private final RepairService repairService;

    /**
     * 构造 RepairController，初始化当前实例。
     * @param repairService repair服务参数。
     */
    public RepairController(RepairService repairService) {
        this.repairService = repairService;
    }

    /**
     * 列出Repairs。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/repairs")
    public Result listRepairs() {
        return Result.success("获取报修列表成功", repairService.listRepairs());
    }

    /**
     * 创建报修。
     * @param repair 报修参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/repairs")
    public Result createRepair(@RequestBody RepairEntity repair) {
        throw retiredWorkflow();
    }

    /**
     * 更新报修。
     * @param id 标识参数。
     * @param repair 报修参数。
     * @return Result 类型的处理结果。
     */
    @PutMapping("/repairs/{id}")
    public Result updateRepair(@PathVariable String id, @RequestBody RepairEntity repair) {
        throw retiredWorkflow();
    }

    /**
     * 删除报修。
     * @param id 标识参数。
     * @return Result 类型的处理结果。
     */
    @DeleteMapping("/repairs/{id}")
    public Result deleteRepair(@PathVariable String id) {
        throw retiredWorkflow();
    }

    /**
     * replaceRepairs的处理逻辑。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @PutMapping("/repairs/batch")
    public Result replaceRepairs(@RequestBody RepairBatchRequest request) {
        throw retiredWorkflow();
    }

    /**
     * 重置Repairs。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/repairs/reset")
    public Result resetRepairs() {
        throw retiredWorkflow();
    }

    /**
     * assignRepair的处理逻辑。
     * @param id 标识参数。
     * @param repair 报修参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/repairs/{id}/assign")
    public Result assignRepair(@PathVariable String id, @RequestBody(required = false) RepairEntity repair) {
        throw retiredWorkflow();
    }

    /**
     * 提交Acceptance。
     * @param id 标识参数。
     * @param repair 报修参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/repairs/{id}/submit-acceptance")
    public Result submitAcceptance(@PathVariable String id, @RequestBody(required = false) RepairEntity repair) {
        throw retiredWorkflow();
    }

    /**
     * 完成报修。
     * @param id 标识参数。
     * @param repair 报修参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/repairs/{id}/complete")
    public Result completeRepair(@PathVariable String id, @RequestBody(required = false) RepairEntity repair) {
        throw retiredWorkflow();
    }

    /**
     * retiredWorkflow的处理逻辑。
     * @return ResponseStatusException 类型的处理结果。
     */
    private ResponseStatusException retiredWorkflow() {
        return new ResponseStatusException(HttpStatus.GONE, RETIRED_WORKFLOW_MESSAGE);
    }

    @Data
    public static class RepairBatchRequest {
        private String resource;
        private List<RepairEntity> records;
    }
}
