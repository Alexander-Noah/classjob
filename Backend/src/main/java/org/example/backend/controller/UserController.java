/*
 * 用户的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/auth/login、/auth/logout、/auth/profile、/auth/profile、/users/options；完整路径由类级与方法级映射组合。
 * 主要协作组件：UserService、JwtService。
 */
package org.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.entity.UserEntity;
import org.example.backend.result.Result;
import org.example.backend.security.JwtAuthenticationFilter;
import org.example.backend.security.JwtService;
import org.example.backend.security.LoginAttemptLimiter;
import org.example.backend.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@CrossOrigin
@RestController
public class UserController {
    private final UserService userService;
    private final JwtService jwtService;
    private final LoginAttemptLimiter loginAttemptLimiter;

    /**
     * 构造 UserController，初始化当前实例。
     * @param userService 用户服务参数。
     * @param jwtService JWT服务参数。
     * @param loginAttemptLimiter 登录尝试Limiter参数。
     */
    public UserController(UserService userService, JwtService jwtService, LoginAttemptLimiter loginAttemptLimiter) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.loginAttemptLimiter = loginAttemptLimiter;
    }

    /**
     * 登录的处理逻辑。
     * @param params 参数参数。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/auth/login")
    public Result login(@RequestBody(required = false) Map<String, String> params, HttpServletRequest request) {
        String username = params == null ? "" : params.get("username");
        String password = params == null ? "" : params.get("password");
        loginAttemptLimiter.assertAllowed(username, request);
        UserEntity user = userService.login(username, password);
        if (user == null) {
            loginAttemptLimiter.recordFailure(username, request);
            return Result.error("用户名或密码错误");
        }

        loginAttemptLimiter.recordSuccess(username, request);
        String token = jwtService.generateToken(user);
        return Result.success(Map.of(
                "token", token,
                "role", user.getRoleCode(),
                "user", sanitizeUser(user)
        ));
    }

    /**
     * 退出登录的处理逻辑。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/auth/logout")
    public Result logout() {
        return Result.success("退出登录成功");
    }

    /**
     * profile的处理逻辑。
     * @param username username参数。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/auth/profile")
    public Result profile(
            @RequestParam(value = "username", required = false) String username,
            HttpServletRequest request
    ) {
        String authUsername = stringAttribute(request, JwtAuthenticationFilter.AUTH_USERNAME_ATTRIBUTE);
        String authRole = stringAttribute(request, JwtAuthenticationFilter.AUTH_ROLE_ATTRIBUTE);
        String targetUsername = username == null || username.isBlank() ? authUsername : username;

        if (targetUsername == null || targetUsername.isBlank()) {
            return Result.error(401, "登入状态无效");
        }

        if (authUsername != null && !authUsername.equals(targetUsername) && !"systemAdmin".equals(authRole)) {
            return Result.error(403, "无权查看其他账号资料");
        }

        UserEntity user = userService.profile(targetUsername);
        if (user != null) {
        return Result.success("获取个人资料成功", sanitizeUser(user));
        }
        return Result.error("用户不存在");
    }

    /**
     * 更新Profile。
     * @param profile profile参数。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @PutMapping("/auth/profile")
    public Result updateProfile(@RequestBody(required = false) UserEntity profile, HttpServletRequest request) {
        String authUsername = stringAttribute(request, JwtAuthenticationFilter.AUTH_USERNAME_ATTRIBUTE);
        if (authUsername == null || authUsername.isBlank()) {
            return Result.error(401, "登录状态无效");
        }

        return Result.success("更新个人资料成功", sanitizeUser(userService.updateProfile(authUsername, profile)));
    }

    /**
     * 列出用户。
     * @return Result 类型的处理结果。
     */
    @GetMapping({"/users", "/admin/users"})
    public Result listUsers() {
        return Result.success("获取用户列表成功", sanitizeUsers(userService.listUsers()));
    }

    /**
     * 列出用户选项。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/users/options")
    public Result listUserOptions() {
        return Result.success("获取用户选项成功", userService.listUsers().stream().map(this::toUserOption).toList());
    }

    /**
     * 创建用户。
     * @param user 用户参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping({"/users", "/admin/users"})
    public Result createUser(@RequestBody UserEntity user) {
        return Result.success("新增用户成功", sanitizeUser(userService.createUser(user)));
    }

    /**
     * 更新用户。
     * @param id 标识参数。
     * @param user 用户参数。
     * @return Result 类型的处理结果。
     */
    @PutMapping({"/users/{id}", "/admin/users/{id}"})
    public Result updateUser(@PathVariable Integer id, @RequestBody UserEntity user) {
        return Result.success("更新用户成功", sanitizeUser(userService.updateUser(id, user)));
    }

    /**
     * 更新用户状态。
     * @param id 标识参数。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping({"/users/{id}/status", "/admin/users/{id}/status"})
    public Result updateUserStatus(@PathVariable Integer id, @RequestBody(required = false) Map<String, Integer> payload) {
        Integer status = payload == null ? 1 : payload.get("status");
        return Result.success("更新用户状态成功", sanitizeUser(userService.updateUserStatus(id, status)));
    }

    /**
     * 删除用户。
     * @param id 标识参数。
     * @return Result 类型的处理结果。
     */
    @DeleteMapping({"/users/{id}", "/admin/users/{id}"})
    public Result deleteUser(@PathVariable Integer id) {
        return Result.success("删除用户成功", sanitizeUser(userService.deleteUser(id)));
    }

    /**
     * 重置用户密码。
     * @param id 标识参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping({"/users/{id}/reset-password", "/admin/users/{id}/reset-password"})
    public Result resetUserPassword(@PathVariable Integer id) {
        return Result.success("重置密码成功", sanitizeUser(userService.resetUserPassword(id)));
    }

    /**
     * batchEnable用户的处理逻辑。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping({"/users/batch/enable", "/admin/users/batch/enable"})
    public Result batchEnableUsers(@RequestBody(required = false) Map<String, Object> payload) {
        return Result.success("批量启用成功", sanitizeUsers(userService.batchUpdateStatus(extractIds(payload), 1)));
    }

    /**
     * batchDisable用户的处理逻辑。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping({"/users/batch/disable", "/admin/users/batch/disable"})
    public Result batchDisableUsers(@RequestBody(required = false) Map<String, Object> payload) {
        return Result.success("批量停用成功", sanitizeUsers(userService.batchUpdateStatus(extractIds(payload), 0)));
    }

    /**
     * batchDelete用户的处理逻辑。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping({"/users/batch/delete", "/admin/users/batch/delete"})
    public Result batchDeleteUsers(@RequestBody(required = false) Map<String, Object> payload) {
        return Result.success("批量删除成功", sanitizeUsers(userService.batchDeleteUsers(extractIds(payload))));
    }

    /**
     * batchUpdate角色的处理逻辑。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping({"/users/batch/role", "/admin/users/batch/role"})
    public Result batchUpdateRole(@RequestBody(required = false) Map<String, Object> payload) {
        String roleName = stringPayload(payload, "role");
        String roleCode = stringPayload(payload, "roleCode");
        return Result.success("批量分配角色成功", sanitizeUsers(userService.batchUpdateRole(extractIds(payload), roleName, roleCode)));
    }

    /**
     * batchUpdate部门的处理逻辑。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping({"/users/batch/department", "/admin/users/batch/department"})
    public Result batchUpdateDepartment(@RequestBody(required = false) Map<String, Object> payload) {
        return Result.success("批量调整部门成功", sanitizeUsers(userService.batchUpdateDepartment(extractIds(payload), stringPayload(payload, "department"))));
    }

    /**
     * 字符串Attribute的处理逻辑。
     * @param request 请求参数。
     * @param name 名称参数。
     * @return String 类型的处理结果。
     */
    private String stringAttribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 字符串载荷的处理逻辑。
     * @param payload 载荷参数。
     * @param name 名称参数。
     * @return String 类型的处理结果。
     */
    private String stringPayload(Map<String, Object> payload, String name) {
        Object value = payload == null ? null : payload.get(name);
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 提取标识集合。
     * @param payload 载荷参数。
     * @return 当前操作得到的记录集合。
     */
    private List<Integer> extractIds(Map<String, Object> payload) {
        Object rawIds = payload == null ? null : payload.get("ids");
        if (!(rawIds instanceof List<?> ids)) {
            return List.of();
        }

        return ids.stream()
                .map(this::toInteger)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 转换为Integer。
     * @param value 值参数。
     * @return Integer 类型的处理结果。
     */
    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 转换为用户选项。
     * @param user 用户参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, Object> toUserOption(UserEntity user) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("id", user.getId());
        option.put("username", user.getUsername());
        option.put("realName", user.getRealName());
        option.put("roleCode", user.getRoleCode());
        option.put("roleName", user.getRoleName());
        option.put("department", user.getDepartment());
        option.put("status", user.getStatus());
        return option;
    }

    /**
     * 清洗用户。
     * @param user 用户参数。
     * @return UserEntity 类型的处理结果。
     */
    private UserEntity sanitizeUser(UserEntity user) {
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    /**
     * 清洗用户。
     * @param users 用户参数。
     * @return 当前操作得到的记录集合。
     */
    private List<UserEntity> sanitizeUsers(List<UserEntity> users) {
        if (users != null) {
            users.forEach(this::sanitizeUser);
        }
        return users;
    }
}
