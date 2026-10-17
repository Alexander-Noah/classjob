/*
 * 实验室预约的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/reservations、/reservations/{id:\\d+}、/reservations、/reservations/{id:\\d+}/approve、/reservations/{id:\\d+}/reject；完整路径由类级与方法级映射组合。
 * 主要协作组件：ReservationCatalogQuery。
 */
package org.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.security.AuthenticationAttributes;
import org.example.backend.service.ReservationAccessScope;
import org.example.backend.service.ReservationActor;
import org.example.backend.service.ReservationCatalogQuery;
import org.example.backend.service.ReservationCommandHandler;
import org.example.backend.service.ReviewReservationCommand;
import org.example.backend.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@CrossOrigin
@RestController
public final class ReservationController {
    private final ReservationCatalogQuery catalog;
    private final ReservationCommandHandler commands;

    public ReservationController(ReservationCatalogQuery catalog, ReservationCommandHandler commands) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.commands = Objects.requireNonNull(commands, "commands");
    }

    @GetMapping("/reservations")
    public Result list(HttpServletRequest request) {
        ReservationActor actor = actor(request, false);
        return Result.success("获取预约列表成功", catalog.list(ReservationAccessScope.forActor(actor)).stream()
                .map(ReservationResponse::from).toList());
    }

    @GetMapping("/reservations/{id:\\d+}")
    public Result detail(@PathVariable long id, HttpServletRequest request) {
        ReservationActor actor = actor(request, false);
        return Result.success("获取预约详情成功", ReservationResponse.from(
                catalog.findById(id, ReservationAccessScope.forActor(actor))));
    }

    @PostMapping("/reservations")
    public Result create(@RequestBody ReservationCreateRequest request, HttpServletRequest servletRequest) {
        ReservationActor actor = actor(servletRequest, false);
        return Result.success("新增预约成功", ReservationResponse.from(commands.create(request.toCommand(), actor)));
    }

    @PostMapping("/reservations/{id:\\d+}/approve")
    public Result approve(@PathVariable long id, @RequestBody(required = false) ReservationReviewRequest request,
                          HttpServletRequest servletRequest) {
        return Result.success("通过预约成功", ReservationResponse.from(commands.approve(
                new ReviewReservationCommand(id, request == null ? null : request.reviewRemark()), actor(servletRequest, true))));
    }

    @PostMapping("/reservations/{id:\\d+}/reject")
    public Result reject(@PathVariable long id, @RequestBody(required = false) ReservationReviewRequest request,
                         HttpServletRequest servletRequest) {
        return Result.success("驳回预约成功", ReservationResponse.from(commands.reject(
                new ReviewReservationCommand(id, request == null ? null : request.reviewRemark()), actor(servletRequest, true))));
    }

    private ReservationActor actor(HttpServletRequest request, boolean reviewerRequired) {
        Object rawUserId = request.getAttribute(AuthenticationAttributes.USER_ID);
        Object rawRole = request.getAttribute(AuthenticationAttributes.ROLE);
        String username = stringAttribute(request, AuthenticationAttributes.USERNAME);
        long userId = positiveUserId(rawUserId);
        if (reviewerRequired && (username == null || username.isBlank())) {
            throw forbidden();
        }
        String role = rawRole == null ? null : String.valueOf(rawRole);
        if ("systemAdmin".equals(role)) {
            return ReservationActor.systemAdministrator(userId, username);
        }
        if ("labAdmin".equals(role)) {
            return ReservationActor.laboratoryAdministrator(userId, username);
        }
        if ("teacher".equals(role)) {
            return ReservationActor.teacher(userId, username);
        }
        throw forbidden();
    }

    private long positiveUserId(Object rawUserId) {
        if (rawUserId == null) {
            throw forbidden();
        }
        try {
            long userId = Long.parseLong(String.valueOf(rawUserId));
            if (userId <= 0) {
                throw forbidden();
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw forbidden();
        }
    }

    private String stringAttribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value == null ? null : String.valueOf(value);
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated reservation identity is required");
    }
}
