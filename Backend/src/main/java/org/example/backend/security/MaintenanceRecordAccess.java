package org.example.backend.security;

import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MaintenanceRecordAccess {
    private static final Set<String> WRITE = Set.of("维护记录填写", "维护记录管理", "故障报修");
    private MaintenanceRecordAccess() { }

    public static boolean canWrite(String role, String permissions) {
        if ("systemAdmin".equals(role)) return true;
        if (!"labAdmin".equals(role)) return false;
        if (permissions == null || permissions.isBlank()) return true;
        try {
            Object parsed = new ObjectMapper().readValue(permissions, Object.class);
            if (!(parsed instanceof List<?> groups)) return false;
            if (groups.isEmpty()) return true;
            return groups.stream().anyMatch(group -> group instanceof String item ? WRITE.contains(item)
                    : group instanceof Map<?,?> map && map.get("items") instanceof List<?> items
                    && items.stream().anyMatch(WRITE::contains));
        } catch (RuntimeException exception) { return false; }
    }
}
