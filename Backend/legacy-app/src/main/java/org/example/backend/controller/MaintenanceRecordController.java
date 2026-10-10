package org.example.backend.controller;

import org.example.backend.entity.MaintenanceRecord;
import org.example.backend.result.Result;
import org.example.backend.service.MaintenanceRecordService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class MaintenanceRecordController {
    private final MaintenanceRecordService maintenanceRecordService;

    public MaintenanceRecordController(MaintenanceRecordService maintenanceRecordService) {
        this.maintenanceRecordService = maintenanceRecordService;
    }

    @GetMapping("/maintenance-records")
    public Result listMaintenanceRecords() {
        return Result.success("获取维护记录列表成功", maintenanceRecordService.listMaintenanceRecords());
    }

    @GetMapping("/maintenance-records/{id}")
    public Result getMaintenanceRecord(@PathVariable String id) {
        return Result.success("获取维护记录成功", maintenanceRecordService.getMaintenanceRecord(id));
    }

    @PostMapping("/maintenance-records")
    public Result createMaintenanceRecord(@RequestBody MaintenanceRecord record) {
        return Result.success("新增维护记录成功", maintenanceRecordService.createMaintenanceRecord(record));
    }

    @PutMapping("/maintenance-records/{id}")
    public Result updateMaintenanceRecord(@PathVariable String id, @RequestBody MaintenanceRecord record) {
        return Result.success("更新维护记录成功", maintenanceRecordService.updateMaintenanceRecord(id, record));
    }

    @DeleteMapping("/maintenance-records/{id}")
    public Result deleteMaintenanceRecord(@PathVariable String id) {
        maintenanceRecordService.deleteMaintenanceRecord(id);
        return Result.success("删除维护记录成功");
    }
}
