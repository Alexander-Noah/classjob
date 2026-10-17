/*
 * AI 数据访问数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import java.util.Set;

public record AiDataActor(Integer userId, String role, String name, Set<String> permissions) {
    /**
     * 系统管理工作台的处理逻辑。
     * @return 当前判断的布尔结果。
     */
    public boolean systemAdmin() { return "systemAdmin".equals(role); }
    /**
     * 实验室Scope的处理逻辑。
     * @return LabAccessScope 类型的处理结果。
     */
    public LabAccessScope labScope() {
        return systemAdmin() ? LabAccessScope.all() : LabAccessScope.managedBy(userId);
    }
    /**
     * 预约操作者的处理逻辑。
     * @return ReservationActor 类型的处理结果。
     */
    public ReservationActor reservationActor() {
        return switch (role) {
            case "systemAdmin" -> ReservationActor.systemAdministrator(userId, name);
            case "labAdmin" -> ReservationActor.laboratoryAdministrator(userId, name);
            case "teacher" -> ReservationActor.teacher(userId, name);
            default -> throw new IllegalArgumentException("无权查询预约数据");
        };
    }
}
