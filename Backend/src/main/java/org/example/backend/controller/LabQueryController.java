/*
 * 实验室的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/labs、/labs/{id:\\d+}；完整路径由类级与方法级映射组合。
 * 主要协作组件：LabCatalogQuery。
 */
package org.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.service.LabAccessScope;
import org.example.backend.service.LabCatalogQuery;
import org.example.backend.security.AuthenticationAttributes;
import org.example.backend.result.Result;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@CrossOrigin
@RestController
public final class LabQueryController {
    private final LabCatalogQuery query;

    /**
     * 构造 LabQueryController，初始化当前实例。
     * @param query 查询参数。
     */
    public LabQueryController(LabCatalogQuery query) {
        this.query = Objects.requireNonNull(query, "query");
    }

    /**
     * 获取实验室。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/labs")
    public Result getLabs(HttpServletRequest request) {
        return Result.success("查询实验室列表成功", query.list(resolveScope(request)));
    }

    /**
     * 获取实验室By标识。
     * @param id 标识参数。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/labs/{id:\\d+}")
    public Result getLabById(@PathVariable long id, HttpServletRequest request) {
        return Result.success("查询实验室详情成功", query.findById(id, resolveScope(request)));
    }

    /**
     * 解析Scope。
     * @param request 请求参数。
     * @return LabAccessScope 类型的处理结果。
     */
    private LabAccessScope resolveScope(HttpServletRequest request) {
        Object role = request.getAttribute(AuthenticationAttributes.ROLE);
        if ("systemAdmin".equals(String.valueOf(role))) {
            return LabAccessScope.all();
        }

        Object rawUserId = request.getAttribute(AuthenticationAttributes.USER_ID);
        if (rawUserId == null) {
            throw forbiddenIdentity();
        }
        try {
            long userId = Long.parseLong(String.valueOf(rawUserId));
            if (userId <= 0) {
                throw forbiddenIdentity();
            }
            return LabAccessScope.managedBy(userId);
        } catch (NumberFormatException exception) {
            throw forbiddenIdentity();
        }
    }

    /**
     * forbidden身份的处理逻辑。
     * @return ResponseStatusException 类型的处理结果。
     */
    private ResponseStatusException forbiddenIdentity() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user id is required");
    }
}
