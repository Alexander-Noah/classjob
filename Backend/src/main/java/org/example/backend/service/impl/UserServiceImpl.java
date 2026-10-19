/*
 * 用户实现模块。
 * 主要协作组件：UserMapper、BusinessLoopService、PasswordService。
 */
package org.example.backend.service.impl;

import org.example.backend.entity.UserEntity;
import org.example.backend.mapper.UserMapper;
import org.example.backend.security.PasswordService;
import org.example.backend.security.RetiredRoles;
import org.example.backend.service.BusinessLoopService;
import org.example.backend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class UserServiceImpl implements UserService {
    private static final String RETIRED_ROLE_MESSAGE = "角色已停用，请联系系统管理员重新分配角色";
    private final UserMapper userMapper;
    private final BusinessLoopService businessLoopService;
    private final PasswordService passwordService;

    /**
     * 构造 UserServiceImpl，初始化当前实例。
     * @param userMapper 用户数据映射器参数。
     * @param businessLoopService businessLoop服务参数。
     * @param passwordService 密码服务参数。
     */
    public UserServiceImpl(UserMapper userMapper, BusinessLoopService businessLoopService, PasswordService passwordService) {
        this.userMapper = userMapper;
        this.businessLoopService = businessLoopService;
        this.passwordService = passwordService;
    }

    /**
     * 登录的处理逻辑。
     * @param username username参数。
     * @param password 密码参数。
     * @return UserEntity 类型的处理结果。
     */
    @Override
    public UserEntity login(String username, String password) {
        UserEntity user = userMapper.login(username);
        if (user != null && passwordService.matches(password, user.getPassword()) && Integer.valueOf(1).equals(user.getStatus())) {
            assertRoleActive(user.getRoleCode(), user.getRoleName());
            if (passwordService.needsRehash(user.getPassword())) {
                String encodedPassword = passwordService.encode(password);
                userMapper.updatePassword(user.getId(), encodedPassword);
                user.setPassword(encodedPassword);
            }
            return user;
        }
        return null;
    }

    /**
     * profile的处理逻辑。
     * @param username username参数。
     * @return UserEntity 类型的处理结果。
     */
    @Override
    public UserEntity profile(String username) {
        UserEntity user = userMapper.profile(username);
        if (user != null) {
            assertRoleActive(user.getRoleCode(), user.getRoleName());
        }
        return user;
    }

    /**
     * 更新Profile。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param username username参数。
     * @param profile profile参数。
     * @return UserEntity 类型的处理结果。
     */
    @Override
    @Transactional
    public UserEntity updateProfile(String username, UserEntity profile) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("登录状态无效");
        }

        UserEntity existingUser = userMapper.profile(username);
        if (existingUser == null) {
            throw new IllegalArgumentException("未知用户");
        }

        UserEntity editableProfile = profile == null ? new UserEntity() : profile;
        UserEntity nextUser = new UserEntity();
        nextUser.setUsername(existingUser.getUsername());
        nextUser.setRealName(firstNonBlank(editableProfile.getRealName(), existingUser.getRealName(), existingUser.getUsername()));
        nextUser.setDepartment(editableValue(editableProfile.getDepartment(), existingUser.getDepartment()));
        nextUser.setPhone(editableValue(editableProfile.getPhone(), existingUser.getPhone()));
        nextUser.setEmail(editableValue(editableProfile.getEmail(), existingUser.getEmail()));
        nextUser.setAvatarUrl(editableValue(editableProfile.getAvatarUrl(), existingUser.getAvatarUrl()));
        nextUser.setStatus(existingUser.getStatus());
        nextUser.setPermissions(existingUser.getPermissions());
        nextUser.setRoleCode(existingUser.getRoleCode());
        nextUser.setRoleName(existingUser.getRoleName());

        return updateUser(existingUser.getId(), nextUser);
    }

    /**
     * 列出用户。
     * @return 当前操作得到的记录集合。
     */
    @Override
    public List<UserEntity> listUsers() {
        List<UserEntity> users = userMapper.listUsers();
        return users == null ? List.of() : users;
    }

    /**
     * 创建用户。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param user 用户参数。
     * @return UserEntity 类型的处理结果。
     */
    @Override
    @Transactional
    public UserEntity createUser(UserEntity user) {
        normalizeUser(user, true, true);
        user.setPassword(passwordService.encode(user.getPassword()));
        userMapper.insertUser(user);
        saveRole(user.getId(), user.getRoleCode());
        UserEntity savedUser = userMapper.getUser(user.getId());
        recordUserEvent("create", savedUser);
        return savedUser;
    }

    /**
     * 更新用户。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param id 标识参数。
     * @param user 用户参数。
     * @return UserEntity 类型的处理结果。
     */
    @Override
    @Transactional
    public UserEntity updateUser(Integer id, UserEntity user) {
        UserEntity existingUser = userMapper.getUser(id);
        if (existingUser == null) {
            throw new IllegalArgumentException("未知用户");
        }
        boolean hasNewPassword = user.getPassword() != null && !user.getPassword().isBlank();
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            user.setUsername(existingUser.getUsername());
        }
        if (!hasNewPassword) {
            user.setPassword(existingUser.getPassword());
        }
        if (user.getStatus() == null) {
            user.setStatus(existingUser.getStatus());
        }
        if (user.getPermissions() == null) {
            user.setPermissions(existingUser.getPermissions());
        }
        if (user.getAvatarUrl() == null) {
            user.setAvatarUrl(existingUser.getAvatarUrl());
        }
        normalizeUser(user, false, hasNewPassword);
        if (hasNewPassword && !passwordService.isEncoded(user.getPassword())) {
            user.setPassword(passwordService.encode(user.getPassword()));
        }
        int updatedCount = userMapper.updateUser(id, user);
        if (updatedCount == 0) {
            throw new IllegalArgumentException("未知用户");
        }
        saveRole(id, user.getRoleCode());
        UserEntity savedUser = userMapper.getUser(id);
        recordUserEvent("update", savedUser);
        return savedUser;
    }

    /**
     * 更新用户状态。
     * @param id 标识参数。
     * @param status 状态参数。
     * @return UserEntity 类型的处理结果。
     */
    @Override
    public UserEntity updateUserStatus(Integer id, Integer status) {
        int updatedCount = userMapper.updateUserStatus(id, status == null ? 1 : status);
        if (updatedCount == 0) {
            throw new IllegalArgumentException("未知用户");
        }
        UserEntity savedUser = userMapper.getUser(id);
        recordUserEvent(Integer.valueOf(1).equals(savedUser.getStatus()) ? "enable" : "disable", savedUser);
        return savedUser;
    }

    /**
     * 重置用户密码。
     * @param id 标识参数。
     * @return UserEntity 类型的处理结果。
     */
    @Override
    public UserEntity resetUserPassword(Integer id) {
        UserEntity existingUser = requireUser(id);
        userMapper.updatePassword(id, passwordService.encode("Reset1234"));
        UserEntity savedUser = userMapper.getUser(id);
        recordUserEvent("reset-password", savedUser == null ? existingUser : savedUser);
        return savedUser;
    }

    /**
     * 删除用户。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param id 标识参数。
     * @return UserEntity 类型的处理结果。
     */
    @Override
    @Transactional
    public UserEntity deleteUser(Integer id) {
        UserEntity existingUser = requireUser(id);
        assertNoBusinessReferences(List.of(existingUser));
        return deleteExistingUser(existingUser);
    }

    /**
     * 删除Existing用户。
     * @param existingUser existing用户参数。
     * @return UserEntity 类型的处理结果。
     */
    private UserEntity deleteExistingUser(UserEntity existingUser) {
        assertCanDelete(existingUser);
        userMapper.deleteUserRoles(existingUser.getId());
        int deletedCount = userMapper.deleteUser(existingUser.getId());
        if (deletedCount == 0) {
            throw new IllegalArgumentException("未知用户");
        }
        recordUserEvent("delete", existingUser);
        return existingUser;
    }

    /**
     * batchUpdate状态的处理逻辑。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param ids 标识集合参数。
     * @param status 状态参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    @Transactional
    public List<UserEntity> batchUpdateStatus(List<Integer> ids, Integer status) {
        return normalizeIds(ids).stream()
                .map(/** 将当前条目转换为目标结果。 入参：id。 */ id -> updateUserStatus(id, status))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * batchDelete用户的处理逻辑。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param ids 标识集合参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    @Transactional
    public List<UserEntity> batchDeleteUsers(List<Integer> ids) {
        List<UserEntity> usersToDelete = normalizeIds(ids).stream()
                .map(this::requireUser)
                .toList();
        usersToDelete.forEach(this::assertCanDelete);
        assertNoBusinessReferences(usersToDelete);
        return usersToDelete.stream()
                .map(this::deleteExistingUser)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * batchUpdate角色的处理逻辑。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param ids 标识集合参数。
     * @param roleName 角色名称参数。
     * @param roleCode 角色编码参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    @Transactional
    public List<UserEntity> batchUpdateRole(List<Integer> ids, String roleName, String roleCode) {
        String nextRoleCode = roleCode == null || roleCode.isBlank() ? resolveRoleCode(roleName) : roleCode;
        assertRoleActive(nextRoleCode, roleName);
        if (nextRoleCode == null || nextRoleCode.isBlank()) {
            throw new IllegalArgumentException("请选择角色");
        }
        String permissions = defaultPermissions(nextRoleCode);
        String finalRoleCode = nextRoleCode;
        return normalizeIds(ids).stream()
                .map(/** 将当前条目转换为目标结果。 入参：id。 */ id -> {
                    UserEntity existingUser = requireUser(id);
                    if ("systemAdmin".equals(existingUser.getRoleCode()) && !"systemAdmin".equals(finalRoleCode) && userMapper.countSystemAdmins() <= 1) {
                        throw new IllegalArgumentException("最后一个系统管理员不能降级");
                    }
                    saveRole(id, finalRoleCode);
                    userMapper.updateUserPermissions(id, permissions);
                    UserEntity savedUser = userMapper.getUser(id);
                    recordUserEvent("batch-role", savedUser);
                    return savedUser;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * batchUpdate部门的处理逻辑。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param ids 标识集合参数。
     * @param department 部门参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    @Transactional
    public List<UserEntity> batchUpdateDepartment(List<Integer> ids, String department) {
        String nextDepartment = department == null || department.isBlank() ? "未设置" : department;
        return normalizeIds(ids).stream()
                .map(/** 将当前条目转换为目标结果。 入参：id。 */ id -> {
                    int updatedCount = userMapper.updateUserDepartment(id, nextDepartment);
                    if (updatedCount == 0) {
                        throw new IllegalArgumentException("未知用户");
                    }
                    UserEntity savedUser = userMapper.getUser(id);
                    recordUserEvent("batch-department", savedUser);
                    return savedUser;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 要求提供用户。
     * @param id 标识参数。
     * @return UserEntity 类型的处理结果。
     */
    private UserEntity requireUser(Integer id) {
        UserEntity existingUser = userMapper.getUser(id);
        if (existingUser == null) {
            throw new IllegalArgumentException("未知用户");
        }
        return existingUser;
    }

    /**
     * 归一化标识集合。
     * @param ids 标识集合参数。
     * @return 当前操作得到的记录集合。
     */
    private List<Integer> normalizeIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("请选择用户");
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    /**
     * 断言CanDelete。
     * @param user 用户参数。
     */
    private void assertCanDelete(UserEntity user) {
        if ("sysadmin".equalsIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException("系统内置管理员账号不能删除");
        }
        if ("systemAdmin".equals(user.getRoleCode()) && userMapper.countSystemAdmins() <= 1) {
            throw new IllegalArgumentException("最后一个系统管理员不能删除");
        }
    }

    /**
     * 断言NoBusiness引用。
     * @param users 用户参数。
     */
    private void assertNoBusinessReferences(List<UserEntity> users) {
        List<String> referencedNames = users.stream()
                .filter(/** 判断当前条目是否符合筛选条件。 入参：user。 */ user -> userMapper.countUserBusinessReferences(user.getId()) > 0)
                .map(this::displayName)
                .toList();
        if (!referencedNames.isEmpty()) {
            String visibleNames = String.join("、", referencedNames.stream().limit(5).toList());
            String suffix = referencedNames.size() > 5 ? "等 " + referencedNames.size() + " 个用户" : "";
            throw new IllegalArgumentException("选中的用户已被业务数据引用，无法删除：" + visibleNames + suffix + "。请先解除关联或改为停用。");
        }
    }

    /**
     * display名称的处理逻辑。
     * @param user 用户参数。
     * @return String 类型的处理结果。
     */
    private String displayName(UserEntity user) {
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return String.valueOf(user.getId());
    }

    /**
     * editable值的处理逻辑。
     * @param nextValue next值参数。
     * @param currentValue 当前值参数。
     * @return String 类型的处理结果。
     */
    private String editableValue(String nextValue, String currentValue) {
        return nextValue == null ? currentValue : nextValue.trim();
    }

    /**
     * first非空白的处理逻辑。
     * @return String 类型的处理结果。
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    /**
     * 归一化用户。
     * @param user 用户参数。
     * @param creating creating参数。
     * @param hasNewPassword hasNew密码参数。
     */
    private void normalizeUser(UserEntity user, boolean creating, boolean hasNewPassword) {
        if (user == null) {
            throw new IllegalArgumentException("请填写账号信息");
        }
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            user.setUsername("user" + System.currentTimeMillis());
        }
        if (user.getRealName() == null || user.getRealName().isBlank()) {
            user.setRealName(user.getUsername());
        }
        if (creating && (user.getPassword() == null || user.getPassword().isBlank())) {
            throw new IllegalArgumentException("请填写初始密码");
        }
        if (hasNewPassword) {
            validatePassword(user.getPassword());
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        if (user.getRoleCode() == null || user.getRoleCode().isBlank()) {
            user.setRoleCode(resolveRoleCode(user.getRoleName()));
        }
        assertRoleActive(user.getRoleCode(), user.getRoleName());
        if (user.getRoleCode() == null || user.getRoleCode().isBlank()) {
            throw new IllegalArgumentException("请选择角色");
        }
    }

    /**
     * 校验密码。
     * @param password 密码参数。
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("密码至少 8 位，并包含字母和数字");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("密码必须同时包含字母和数字");
        }
        if (password.matches("(?i)^(123456|password|admin123|qwerty\\d*)$")) {
            throw new IllegalArgumentException("密码过于简单，请更换更安全的密码");
        }
    }

    /**
     * 保存角色。
     * @param userId 用户标识参数。
     * @param roleCode 角色编码参数。
     */
    private void saveRole(Integer userId, String roleCode) {
        assertRoleActive(roleCode, null);
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("请选择角色");
        }
        userMapper.deleteUserRoles(userId);
        userMapper.insertUserRole(userId, roleCode);
    }

    /**
     * 解析角色编码。
     * @param roleName 角色名称参数。
     * @return String 类型的处理结果。
     */
    private String resolveRoleCode(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "";
        }
        return switch (roleName) {
            case "系统管理员" -> "systemAdmin";
            case "实验室管理员" -> "labAdmin";
            case "教师" -> "teacher";
            case "维修人员" -> "maintenance";
            case "教务人员" -> "academic";
            default -> roleName;
        };
    }

    /**
     * 提供默认权限。
     * @param roleCode 角色编码参数。
     * @return String 类型的处理结果。
     */
    private String defaultPermissions(String roleCode) {
        return switch (roleCode) {
            case "systemAdmin" -> "[{\"group\":\"平台治理\",\"items\":[\"用户管理\",\"系统设置\",\"激活管理\",\"平台统计\",\"通知公告\"]}]";
            case "labAdmin" -> "[{\"group\":\"资源管理\",\"items\":[\"实验室管理\",\"课表数据\",\"预约管理\"]},{\"group\":\"资产管理\",\"items\":[\"设备资产\",\"耗材库存\",\"设备状态\"]},{\"group\":\"环境运维\",\"items\":[\"环境管理\",\"维护记录填写\",\"维护记录查看\",\"物联网扩展\"]},{\"group\":\"数据分析\",\"items\":[\"使用记录\",\"统计分析\",\"AI智能助手\"]},{\"group\":\"系统通知\",\"items\":[\"通知公告\"]}]";
            case "academic" -> "[{\"group\":\"教务管理\",\"items\":[\"课表数据\",\"通知公告\"]}]";
            default -> "[{\"group\":\"个人工作\",\"items\":[\"教师课表\",\"课程环境\"]}]";
        };
    }

    /**
     * 断言角色活动。
     * @param roleCode 角色编码参数。
     * @param roleName 角色名称参数。
     */
    private void assertRoleActive(String roleCode, String roleName) {
        if (RetiredRoles.contains(roleCode) || RetiredRoles.contains(roleName)) {
            throw new IllegalArgumentException(RETIRED_ROLE_MESSAGE);
        }
    }

    /**
     * 记录用户事件。
     * @param action action参数。
     * @param user 用户参数。
     */
    private void recordUserEvent(String action, UserEntity user) {
        if (user == null) {
            return;
        }
        businessLoopService.recordEvent("user", action, user.getRealName(), Integer.valueOf(1).equals(user.getStatus()) ? "启用" : "停用", Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "role", user.getRoleName() == null ? user.getRoleCode() : user.getRoleName()
        ));
    }
}
