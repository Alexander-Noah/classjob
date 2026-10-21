package org.example.backend.service;

import org.example.backend.security.RetiredRoles;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/** 角色页权限是原有角色接口权限的进一步限制，不能用来跨角色提权。 */
@Service
public class RolePageAccessService {
    public record Page(String path, String role, String title, boolean menu, boolean locked, List<String> api) {}
    public record Policy(String role, List<String> accessiblePaths, List<String> visiblePaths) {}
    private final ModuleRecordService records;
    private final List<Page> catalog;
    private static final Map<String, String> ROLES = Map.of(
            "systemAdmin", "系统管理员（超级管理员）", "labAdmin", "实验室管理员", "teacher", "教师");

    public RolePageAccessService(ModuleRecordService records, ObjectMapper json) throws IOException {
        this.records = records;
        try (var stream = new ClassPathResource("security/role-pages.json").getInputStream()) {
            catalog = List.copyOf(json.readValue(stream, new TypeReference<List<Page>>() {}));
        }
    }

    public List<Page> pages(String role) {
        requireRole(role);
        return catalog.stream().filter(page -> page.role().equals(role)).toList();
    }

    public List<Map<String, Object>> roles() {
        return List.of("systemAdmin", "labAdmin", "teacher").stream().map(role -> Map.<String, Object>of(
                "code", role, "name", ROLES.get(role), "retired", RetiredRoles.contains(role))).toList();
    }

    public Policy current(String role) {
        var pages = pages(role);
        var saved = records.listModuleRecords("role-page-access-" + role);
        if (saved.isEmpty()) {
            return new Policy(role, pages.stream().map(Page::path).toList(),
                    pages.stream().filter(Page::menu).map(Page::path).toList());
        }
        return validate(role, paths(saved.get(0).get("accessiblePaths")), paths(saved.get(0).get("visiblePaths")));
    }

    public Policy save(String role, List<String> accessible, List<String> visible) {
        if (RetiredRoles.contains(role)) throw new IllegalArgumentException("该角色已停用，不能修改授权");
        Policy policy = validate(role, accessible, visible);
        records.replaceModuleRecords("role-page-access-" + role, List.of(Map.of(
                "id", role, "accessiblePaths", policy.accessiblePaths(), "visiblePaths", policy.visiblePaths())));
        return policy;
    }

    private Policy validate(String role, List<String> accessible, List<String> visible) {
        var pages = pages(role);
        if (accessible == null || visible == null) throw new IllegalArgumentException("请提供页面访问和菜单显示配置");
        Set<String> access = new LinkedHashSet<>(accessible), menus = new LinkedHashSet<>(visible);
        Set<String> known = new HashSet<>(), menuPages = new HashSet<>();
        for (Page page : pages) {
            known.add(page.path());
            if (page.menu()) menuPages.add(page.path());
            if (page.locked()) { access.add(page.path()); if (page.menu()) menus.add(page.path()); }
        }
        if (!known.containsAll(access) || !menuPages.containsAll(menus)) throw new IllegalArgumentException("包含未知页面或其他角色的页面");
        if (!access.containsAll(menus)) throw new IllegalArgumentException("显示菜单的页面必须同时允许访问");
        return new Policy(role, List.copyOf(access), List.copyOf(menus));
    }

    private List<String> paths(Object value) {
        if (!(value instanceof List<?> list) || list.stream().anyMatch(item -> !(item instanceof String))) {
            throw new IllegalArgumentException("角色页面权限配置格式无效");
        }
        return list.stream().map(String.class::cast).toList();
    }

    private void requireRole(String role) {
        if (!ROLES.containsKey(role)) throw new IllegalArgumentException("未知角色");
    }

    public boolean isApiAllowed(String method, String path, String role) {
        if ("GET".equalsIgnoreCase(method) && "/auth/page-access".equals(path)) return true;
        // 保留现有无页面角色的接口规则；停用角色由原有认证授权层阻止。
        if (!ROLES.containsKey(role)) return true;
        var candidates = catalog.stream().filter(page -> page.role().equals(role))
                .filter(page -> page.api().stream().anyMatch(rule -> matches(rule, method, path))).toList();
        if (candidates.isEmpty()) return true; // 公共资料/上传/工作台等沿用现有服务端规则。
        var allowed = current(role).accessiblePaths();
        return candidates.stream().anyMatch(page -> allowed.contains(page.path()));
    }

    private boolean matches(String rule, String method, String path) {
        String[] parts = rule.split(" ", 2);
        if (!parts[0].equals("*") && !parts[0].equalsIgnoreCase(method)) return false;
        String route = parts[1];
        if (route.endsWith("/**")) {
            String prefix = route.substring(0, route.length() - 3);
            return path.equals(prefix) || path.startsWith(prefix + "/");
        }
        String regex = Arrays.stream(route.split("\\*", -1)).map(Pattern::quote)
                .reduce((left, right) -> left + "[^/]+" + right).orElse("");
        return path.matches(regex);
    }
}
