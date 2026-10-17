/*
 * 实验室的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/labs/{id:\\d+}/devices、/labs、/labs/{id:\\d+}、/labs/batch、/labs/{id:\\d+}；完整路径由类级与方法级映射组合。
 * 主要协作组件：LabService、DevicesService。
 */
package org.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.entity.LabEntity;
import org.example.backend.result.Result;
import org.example.backend.security.JwtAuthenticationFilter;
import org.example.backend.service.DevicesService;
import org.example.backend.service.LabService;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@CrossOrigin
@RestController
@Slf4j
public class LabController {
    private final LabService labService;
    private final DevicesService deviceService;

    public LabController(LabService labService, DevicesService deviceService) {
        this.labService = labService;
        this.deviceService = deviceService;
    }

    @GetMapping("/labs/{id:\\d+}/devices")
    public Result getLabDevices(@PathVariable Integer id) {
        log.info("查询实验室设备列表，实验室ID：{}", id);
        return Result.success("查询实验室设备成功", deviceService.getDevicesByLabId(id));
    }

    @PostMapping("/labs")
    public Result addLab(@RequestBody LabEntity lab) {
        return Result.success("添加实验室成功", labService.addLab(lab));
    }

    @PutMapping("/labs/{id:\\d+}")
    public Result updateLab(@PathVariable Integer id, @RequestBody LabEntity lab) {
        labService.updateLab(id, lab);
        return Result.success("修改实验室成功", labService.getLabById(id));
    }

    @PutMapping("/labs/batch")
    public Result updateLabs(@RequestBody LabBatchRequest request) {
        List<LabEntity> records = request == null || request.getRecords() == null
                ? Collections.emptyList()
                : request.getRecords();
        return Result.success("批量更新实验室成功", labService.updateLabs(records));
    }

    @DeleteMapping("/labs/{id:\\d+}")
    public Result deleteLab(@PathVariable Integer id) {
        labService.deleteLab(id);
        return Result.success("删除实验室成功");
    }

    @PostMapping("/labs/reset")
    public Result resetLabs(HttpServletRequest request) {
        return Result.success("重置实验室成功", labService.getLabs(resolveScopedManagerId(request)));
    }

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

    public static class LabBatchRequest {
        private String resource;
        private List<LabEntity> records;

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public List<LabEntity> getRecords() {
            return records;
        }

        public void setRecords(List<LabEntity> records) {
            this.records = records;
        }
    }
}
