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

    public RepairController(RepairService repairService) {
        this.repairService = repairService;
    }

    @GetMapping("/repairs")
    public Result listRepairs() {
        return Result.success("获取报修列表成功", repairService.listRepairs());
    }

    @PostMapping("/repairs")
    public Result createRepair(@RequestBody RepairEntity repair) {
        throw retiredWorkflow();
    }

    @PutMapping("/repairs/{id}")
    public Result updateRepair(@PathVariable String id, @RequestBody RepairEntity repair) {
        throw retiredWorkflow();
    }

    @DeleteMapping("/repairs/{id}")
    public Result deleteRepair(@PathVariable String id) {
        throw retiredWorkflow();
    }

    @PutMapping("/repairs/batch")
    public Result replaceRepairs(@RequestBody RepairBatchRequest request) {
        throw retiredWorkflow();
    }

    @PostMapping("/repairs/reset")
    public Result resetRepairs() {
        throw retiredWorkflow();
    }

    @PostMapping("/repairs/{id}/assign")
    public Result assignRepair(@PathVariable String id, @RequestBody(required = false) RepairEntity repair) {
        throw retiredWorkflow();
    }

    @PostMapping("/repairs/{id}/submit-acceptance")
    public Result submitAcceptance(@PathVariable String id, @RequestBody(required = false) RepairEntity repair) {
        throw retiredWorkflow();
    }

    @PostMapping("/repairs/{id}/complete")
    public Result completeRepair(@PathVariable String id, @RequestBody(required = false) RepairEntity repair) {
        throw retiredWorkflow();
    }

    private ResponseStatusException retiredWorkflow() {
        return new ResponseStatusException(HttpStatus.GONE, RETIRED_WORKFLOW_MESSAGE);
    }

    @Data
    public static class RepairBatchRequest {
        private String resource;
        private List<RepairEntity> records;
    }
}
