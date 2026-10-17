/*
 * ModuleRecordController的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/modules/{moduleName}、/batch、/reset；完整路径由类级与方法级映射组合。
 * 主要协作组件：ModuleRecordService。
 */
package org.example.backend.controller;

import lombok.Data;
import org.example.backend.result.Result;
import org.example.backend.service.ModuleRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/modules/{moduleName}")
public class ModuleRecordController {
    private final ModuleRecordService moduleRecordService;

    /**
     * 构造 ModuleRecordController，初始化当前实例。
     * @param moduleRecordService module记录服务参数。
     */
    public ModuleRecordController(ModuleRecordService moduleRecordService) {
        this.moduleRecordService = moduleRecordService;
    }

    /**
     * 列出Module记录集合。
     * @param moduleName module名称参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping
    public Result listModuleRecords(@PathVariable String moduleName) {
        return Result.success("获取模块数据成功", moduleRecordService.listModuleRecords(moduleName));
    }

    /**
     * replaceModule记录集合的处理逻辑。
     * @param moduleName module名称参数。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @PutMapping("/batch")
    public Result replaceModuleRecords(@PathVariable String moduleName, @RequestBody ModuleRecordBatchRequest request) {
        List<Map<String, Object>> records = request == null || request.getRecords() == null
                ? Collections.emptyList()
                : request.getRecords();

        return Result.success("保存模块数据成功", moduleRecordService.replaceModuleRecords(moduleName, records));
    }

    /**
     * 重置Module记录集合。
     * @param moduleName module名称参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/reset")
    public Result resetModuleRecords(@PathVariable String moduleName) {
        return Result.success("重置模块数据成功", moduleRecordService.resetModuleRecords(moduleName));
    }

    @Data
    public static class ModuleRecordBatchRequest {
        private String resource;
        private List<Map<String, Object>> records;
    }
}
