package org.example.backend.security;

import java.util.Locale;
import java.util.Set;

/** 暂停角色的统一入口；保留业务代码和历史账号，不通过迁移删除角色数据。 */
public final class RetiredRoles {
    private static final Set<String> VALUES = Set.of("teacher", "教师", "maintenance", "维修人员");

    /** 工具类无需实例化。 */
    private RetiredRoles() { }

    /** 同时兼容数据库中的角色编码、中文名称和旧令牌中的编码。 */
    public static boolean contains(String value) {
        return value != null && VALUES.contains(value.trim().toLowerCase(Locale.ROOT));
    }
}
