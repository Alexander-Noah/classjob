package org.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.result.Result;
import org.example.backend.security.JwtAuthenticationFilter;
import org.example.backend.service.RolePageAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@RestController
public class RolePageAccessController {
    private final RolePageAccessService access;
    public RolePageAccessController(RolePageAccessService access) { this.access = access; }
    public record Update(List<String> accessiblePaths, List<String> visiblePaths) {}

    @GetMapping("/auth/page-access")
    public Result current(HttpServletRequest request) {
        Object role = request.getAttribute(JwtAuthenticationFilter.AUTH_ROLE_ATTRIBUTE);
        if (role == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return Result.success(access.current(String.valueOf(role)));
    }

    @GetMapping("/role-page-access")
    public Result roles(HttpServletRequest request) {
        admin(request);
        return Result.success(access.roles());
    }

    @GetMapping("/role-page-access/{role}")
    public Result detail(@PathVariable String role, HttpServletRequest request) {
        admin(request);
        return Result.success(Map.of("pages", access.pages(role), "policy", access.current(role)));
    }

    @PutMapping("/role-page-access/{role}")
    public Result save(@PathVariable String role, @RequestBody Update update, HttpServletRequest request) {
        admin(request);
        return Result.success(access.save(role, update.accessiblePaths(), update.visiblePaths()));
    }

    private void admin(HttpServletRequest request) {
        if (!"systemAdmin".equals(request.getAttribute(JwtAuthenticationFilter.AUTH_ROLE_ATTRIBUTE)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅超级管理员可配置角色菜单权限");
    }
}
