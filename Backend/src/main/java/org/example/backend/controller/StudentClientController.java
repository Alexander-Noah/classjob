/*
 * 学生客户端的 HTTP 接口入口，负责参数接收和响应适配。
 * 主要协作组件：StudentClientService。
 */
package org.example.backend.controller;

import org.example.backend.entity.StudentClientEntity;
import org.example.backend.result.Result;
import org.example.backend.service.StudentClientService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class StudentClientController {
    private final StudentClientService studentClientService;

    /**
     * 构造 StudentClientController，初始化当前实例。
     * @param studentClientService 学生客户端服务参数。
     */
    public StudentClientController(StudentClientService studentClientService) {
        this.studentClientService = studentClientService;
    }

    /**
     * online的处理逻辑。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping({"/student-clients/online", "/api/student-clients/online"})
    public Result online(@RequestBody StudentClientEntity request) {
        return Result.success("student client online status registered", studentClientService.online(request));
    }

    /**
     * offline的处理逻辑。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping({"/student-clients/offline", "/api/student-clients/offline"})
    public Result offline(@RequestBody StudentClientEntity request) {
        studentClientService.offline(request);
        return Result.success("student client offline status registered");
    }

    /**
     * 列出当前操作。
     * @param labId 实验室标识参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping({"/student-clients", "/api/student-clients"})
    public Result list(@RequestParam Long labId) {
        return Result.success("student client list loaded", studentClientService.listByLabId(labId));
    }
}
