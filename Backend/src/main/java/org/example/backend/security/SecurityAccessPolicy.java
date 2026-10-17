/*
 * 安全策略的服务端安全组件；调用方应保持身份认证与权限判断边界。
 */
package org.example.backend.security;

import java.util.Locale;
import java.util.Set;

public class SecurityAccessPolicy {
    private static final Set<String> COMMON_PREFIXES = Set.of(
            "/ai-assistant",
            "/auth/profile",
            "/auth/logout",
            "/dashboard",
            "/files",
            "/teacher-hosts",
            "/student-clients"
    );

    /**
     * 判断是否为Allowed。
     * @param method 方法参数。
     * @param path 路径参数。
     * @param roleCode 角色编码参数。
     * @return 当前判断的布尔结果。
     */
    public boolean isAllowed(String method, String path, String roleCode) {
        String normalizedPath = normalizePath(path);
        String normalizedMethod = normalizeMethod(method);
        String normalizedRole = normalizeRole(roleCode);

        if ("maintenance".equals(normalizedRole)) {
            return false;
        }
        if (isCommonAuthenticatedPath(normalizedPath)) {
            return true;
        }
        if ("systemAdmin".equals(normalizedRole)) {
            return true;
        }
        if ("labAdmin".equals(normalizedRole)) {
            return isUserOptionsPath(normalizedMethod, normalizedPath)
                    || isLabActivationCodePath(normalizedMethod, normalizedPath)
                    || isLabAdminPath(normalizedPath);
        }
        if ("teacher".equals(normalizedRole)) {
            return isTeacherPath(normalizedMethod, normalizedPath);
        }
        if ("academic".equals(normalizedRole)) {
            return isAcademicPath(normalizedPath);
        }
        return false;
    }

    /**
     * 判断是否为Common已认证状态路径。
     * @param path 路径参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isCommonAuthenticatedPath(String path) {
        if (isNoticeUserPath(path)) {
            return true;
        }
        return COMMON_PREFIXES.stream().anyMatch(/** 处理 anyMatch 调用中的回调逻辑。 入参：prefix。 */ prefix -> startsWithSegment(path, prefix));
    }

    /**
     * 判断是否为实验室Admin路径。
     * @param path 路径参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isLabAdminPath(String path) {
        return startsWithAnySegment(path,
                "/academic-schedules",
                "/approval-countersigns",
                "/business-loop",
                "/class-timetables",
                "/consumables",
                "/course-environment-requests",
                "/device-status",
                "/device-inventory-records",
                "/device-transfer-records",
                "/devices",
                "/environment-templates",
                "/iot",
                "/lab-software",
                "/labs",
                "/maintenance-records",
                "/notices",
                "/repairs",
                "/reservations",
                "/schedule-adjustments",
                "/usage-records"
        );
    }

    /**
     * 判断是否为用户选项路径。
     * @param method 方法参数。
     * @param path 路径参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isUserOptionsPath(String method, String path) {
        return "GET".equals(method) && path.equals("/users/options");
    }

    /**
     * 判断是否为实验室Activation编码路径。
     * @param method 方法参数。
     * @param path 路径参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isLabActivationCodePath(String method, String path) {
        if ("GET".equals(method) && path.equals("/activation-codes/lab-bindings")) {
            return true;
        }
        if ("POST".equals(method) && path.equals("/activation-codes/bind-lab")) {
            return true;
        }
        return "POST".equals(method) && path.matches("^/activation-codes/[^/]+/unbind-lab$");
    }

    /**
     * 判断是否为教师路径。
     * @param method 方法参数。
     * @param path 路径参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isTeacherPath(String method, String path) {
        if (startsWithSegment(path, "/course-environment-requests")) {
            return isListOrCreateOrRecordUpdate(method, path, "/course-environment-requests");
        }
        if (startsWithSegment(path, "/schedule-adjustments")) {
            return isListOrCreateOrRecordUpdate(method, path, "/schedule-adjustments");
        }
        if (startsWithSegment(path, "/reservations")) {
            return "GET".equals(method) || ("POST".equals(method) && path.equals("/reservations"));
        }
        return "GET".equals(method) && startsWithAnySegment(path, "/class-timetables", "/notices");
    }

    /**
     * 判断是否为教务路径。
     * @param path 路径参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isAcademicPath(String path) {
        return startsWithAnySegment(path, "/academic-schedules", "/class-timetables", "/notices");
    }

    /**
     * 判断是否为通知用户路径。
     * @param path 路径参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isNoticeUserPath(String path) {
        return startsWithSegment(path, "/notices/user")
                || startsWithSegment(path, "/notices/stats")
                || path.matches("^/notices/[^/]+/read$");
    }

    /**
     * 开始WithAnySegment的处理逻辑。
     * @param path 路径参数。
     * @return 当前判断的布尔结果。
     */
    private boolean startsWithAnySegment(String path, String... prefixes) {
        for (String prefix : prefixes) {
            if (startsWithSegment(path, prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 开始WithSegment的处理逻辑。
     * @param path 路径参数。
     * @param prefix 前缀参数。
     * @return 当前判断的布尔结果。
     */
    private boolean startsWithSegment(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    /**
     * 判断是否为列表OrCreateOr记录Update。
     * @param method 方法参数。
     * @param path 路径参数。
     * @param prefix 前缀参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isListOrCreateOrRecordUpdate(String method, String path, String prefix) {
        if ("GET".equals(method)) {
            return true;
        }
        if ("POST".equals(method)) {
            return path.equals(prefix);
        }
        return "PUT".equals(method) && path.matches("^" + prefix + "/[^/]+$");
    }

    /**
     * 归一化路径。
     * @param path 路径参数。
     * @return String 类型的处理结果。
     */
    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.trim().replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * 归一化方法。
     * @param method 方法参数。
     * @return String 类型的处理结果。
     */
    private String normalizeMethod(String method) {
        return method == null ? "GET" : method.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 归一化角色。
     * @param roleCode 角色编码参数。
     * @return String 类型的处理结果。
     */
    private String normalizeRole(String roleCode) {
        return roleCode == null ? "" : roleCode.trim();
    }
}
