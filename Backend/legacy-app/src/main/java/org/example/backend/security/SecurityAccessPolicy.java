package org.example.backend.security;

import java.util.Locale;
import java.util.Set;

public class SecurityAccessPolicy {
    private static final Set<String> COMMON_PREFIXES = Set.of(
            "/auth/profile",
            "/auth/logout",
            "/dashboard",
            "/files"
    );

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

    private boolean isCommonAuthenticatedPath(String path) {
        if (isNoticeUserPath(path)) {
            return true;
        }
        return COMMON_PREFIXES.stream().anyMatch(prefix -> startsWithSegment(path, prefix));
    }

    private boolean isLabAdminPath(String path) {
        return startsWithAnySegment(path,
                "/academic-schedules",
                "/business-loop",
                "/class-timetables",
                "/device-inventory-records",
                "/device-transfer-records",
                "/devices",
                "/labs",
                "/maintenance-records",
                "/notices",
                "/repairs",
                "/reservations",
                "/usage-records"
        );
    }

    private boolean isUserOptionsPath(String method, String path) {
        return "GET".equals(method) && path.equals("/users/options");
    }

    private boolean isTeacherPath(String method, String path) {
        if (startsWithSegment(path, "/reservations")) {
            return "GET".equals(method) || ("POST".equals(method) && path.equals("/reservations"));
        }
        return "GET".equals(method) && startsWithAnySegment(path, "/class-timetables", "/notices");
    }

    private boolean isAcademicPath(String path) {
        return startsWithAnySegment(path, "/academic-schedules", "/class-timetables", "/notices");
    }

    private boolean isNoticeUserPath(String path) {
        return startsWithSegment(path, "/notices/user")
                || startsWithSegment(path, "/notices/stats")
                || path.matches("^/notices/[^/]+/read$");
    }

    private boolean startsWithAnySegment(String path, String... prefixes) {
        for (String prefix : prefixes) {
            if (startsWithSegment(path, prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean startsWithSegment(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

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

    private String normalizeMethod(String method) {
        return method == null ? "GET" : method.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRole(String roleCode) {
        return roleCode == null ? "" : roleCode.trim();
    }
}
