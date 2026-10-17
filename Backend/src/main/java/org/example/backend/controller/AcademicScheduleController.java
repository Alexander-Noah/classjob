/*
 * 教务课表集成的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/academic-schedules/parse、/academic-schedules/fetch、/academic-schedules/import、/academic-schedules/credentials/{credentialKey}、/academic-schedules/credentials；完整路径由类级与方法级映射组合。
 * 主要协作组件：AcademicScheduleService、AcademicCredentialService。
 */
package org.example.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.backend.entity.AcademicScheduleImportRequest;
import org.example.backend.result.Result;
import org.example.backend.service.AcademicCredentialService;
import org.example.backend.service.AcademicScheduleService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@Slf4j
public class AcademicScheduleController {
    private final AcademicScheduleService academicScheduleService;
    private final AcademicCredentialService academicCredentialService;

    /**
     * 构造 AcademicScheduleController，初始化当前实例。
     * @param academicScheduleService 教务课表服务参数。
     * @param academicCredentialService 教务凭据服务参数。
     */
    public AcademicScheduleController(
            AcademicScheduleService academicScheduleService,
            AcademicCredentialService academicCredentialService
    ) {
        this.academicScheduleService = academicScheduleService;
        this.academicCredentialService = academicCredentialService;
    }

    /**
     * 解析课表。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/academic-schedules/parse")
    public Result parseSchedule(@RequestBody AcademicScheduleImportRequest request) {
        log.info("解析教务课表 HTML");
        return Result.success(academicScheduleService.parseSchedule(request));
    }

    /**
     * 获取课表。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/academic-schedules/fetch")
    public Result fetchSchedule(@RequestBody AcademicScheduleImportRequest request) {
        log.info("抓取教务系统课表");
        return Result.success(academicScheduleService.fetchSchedule(request));
    }

    /**
     * 导入课表。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/academic-schedules/import")
    public Result importSchedule(@RequestBody AcademicScheduleImportRequest request) {
        log.info("导入教务系统课表");
        return Result.success(academicScheduleService.importSchedule(request));
    }

    /**
     * 获取凭据。
     * @param credentialKey 凭据键参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/academic-schedules/credentials/{credentialKey}")
    public Result getCredential(@PathVariable String credentialKey) {
        log.info("查询本地教务账号配置");
        return Result.success(academicCredentialService.getCredentialView(credentialKey));
    }

    /**
     * 保存凭据。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/academic-schedules/credentials")
    public Result saveCredential(@RequestBody AcademicScheduleImportRequest request) {
        log.info("保存本地教务账号配置");
        return Result.success(academicCredentialService.saveCredential(request));
    }

    /**
     * 删除凭据。
     * @param credentialKey 凭据键参数。
     * @return Result 类型的处理结果。
     */
    @DeleteMapping("/academic-schedules/credentials/{credentialKey}")
    public Result deleteCredential(@PathVariable String credentialKey) {
        log.info("删除本地教务账号配置");
        academicCredentialService.deleteCredential(credentialKey);
        return Result.success(true);
    }
}
