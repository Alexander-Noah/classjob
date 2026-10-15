package org.example.backend.controller;

import org.example.backend.entity.MaintenanceRecord;
import org.example.backend.result.Result;
import org.example.backend.service.MaintenanceRecordService;
import org.example.backend.service.UserService;
import org.example.backend.security.AuthenticationAttributes;
import org.example.backend.security.MaintenanceRecordAccess;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
    private final UserService userService;

    public MaintenanceRecordController(MaintenanceRecordService maintenanceRecordService, UserService userService) {
        this.maintenanceRecordService = maintenanceRecordService;
        this.userService = userService;
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
    public Result createMaintenanceRecord(@RequestBody MaintenanceRecord record, HttpServletRequest request) {
        requireWrite(request);
        return Result.success("新增维护记录成功", maintenanceRecordService.createMaintenanceRecord(record));
    }

    @PutMapping("/maintenance-records/{id}")
    public Result updateMaintenanceRecord(@PathVariable String id, @RequestBody MaintenanceRecord record, HttpServletRequest request) {
        requireWrite(request);
        return Result.success("更新维护记录成功", maintenanceRecordService.updateMaintenanceRecord(id, record));
    }

    @DeleteMapping("/maintenance-records/{id}")
    public Result deleteMaintenanceRecord(@PathVariable String id, HttpServletRequest request) {
        requireWrite(request);
        maintenanceRecordService.deleteMaintenanceRecord(id);
        return Result.success("删除维护记录成功");
    }

    private void requireWrite(HttpServletRequest request) {
        Object username = request.getAttribute(AuthenticationAttributes.USERNAME);
        if (username == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        var user = userService.profile(username.toString());
        if (user == null || !Integer.valueOf(1).equals(user.getStatus()) || !MaintenanceRecordAccess.canWrite(user.getRoleCode(), user.getPermissions())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号只有查看权限，不能修改维护记录");
        }
    }
}
