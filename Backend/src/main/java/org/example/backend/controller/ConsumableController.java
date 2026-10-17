/*
 * 耗材库存的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/consumables、/stock-records、/{id:\\d+}/movement、/{id}、/{id}；完整路径由类级与方法级映射组合。
 * 主要协作组件：ConsumableService。
 */
package org.example.backend.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.result.Result;
import org.example.backend.entity.ConsumableEntity;
import org.example.backend.entity.ConsumableStockRecordEntity;
import org.example.backend.service.ConsumableService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/consumables")
public class ConsumableController {
    private final ConsumableService consumableService;

    /**
     * 构造 ConsumableController，初始化当前实例。
     * @param consumableService consumable服务参数。
     */
    public ConsumableController(ConsumableService consumableService) {
        this.consumableService = consumableService;
    }

    /**
     * 列出Consumables。
     * @return Result 类型的处理结果。
     */
    @GetMapping
    public Result listConsumables() {
        return Result.success("获取耗材列表成功", consumableService.listConsumables());
    }

    /**
     * 列出库存记录集合。
     * @param consumableId consumable标识参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/stock-records")
    public Result listStockRecords(@RequestParam(required = false) Long consumableId) {
        return Result.success("获取耗材出入库记录成功", consumableService.listStockRecords(consumableId));
    }

    /**
     * 创建耗材库存。
     * @param consumable 耗材库存参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping
    public Result createConsumable(@RequestBody ConsumableEntity consumable) {
        return Result.success("新增耗材成功", consumableService.createConsumable(consumable));
    }

    /**
     * 记录ConsumableMovement。
     * @param id 标识参数。
     * @param record 记录参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/{id:\\d+}/movement")
    public Result recordConsumableMovement(@PathVariable Long id, @RequestBody ConsumableStockRecordEntity record) {
        return Result.success("保存耗材出入库记录成功", consumableService.recordConsumableMovement(id, record));
    }

    /**
     * 更新耗材库存。
     * @param id 标识参数。
     * @param consumable 耗材库存参数。
     * @return Result 类型的处理结果。
     */
    @PutMapping("/{id}")
    public Result updateConsumable(@PathVariable Long id, @RequestBody ConsumableEntity consumable) {
        return Result.success("更新耗材成功", consumableService.updateConsumable(id, consumable));
    }

    /**
     * 删除耗材库存。
     * @param id 标识参数。
     * @return Result 类型的处理结果。
     */
    @DeleteMapping("/{id}")
    public Result deleteConsumable(@PathVariable Long id) {
        return Result.success("删除耗材成功", consumableService.deleteConsumable(id));
    }

    /**
     * replaceConsumables的处理逻辑。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @PutMapping("/batch")
    public Result replaceConsumables(@RequestBody ConsumableBatchRequest request) {
        List<ConsumableEntity> consumables = request == null || request.getRecords() == null
                ? Collections.emptyList()
                : request.getRecords();
        log.info("replace consumables: {}", consumables.size());
        return Result.success("批量保存耗材成功", consumableService.replaceConsumables(consumables));
    }

    /**
     * 重置Consumables。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/reset")
    public Result resetConsumables() {
        return Result.success("重置耗材数据成功", consumableService.resetConsumables());
    }

    @Data
    public static class ConsumableBatchRequest {
        private String resource;
        private List<ConsumableEntity> records;
    }
}
