package org.example.backend.service;

import java.util.Set;

public record AiDataActor(Integer userId, String role, String name, Set<String> permissions) {
    public boolean systemAdmin() { return "systemAdmin".equals(role); }
    public LabAccessScope labScope() {
        return systemAdmin() ? LabAccessScope.all() : LabAccessScope.managedBy(userId);
    }
    public ReservationActor reservationActor() {
        return switch (role) {
            case "systemAdmin" -> ReservationActor.systemAdministrator(userId, name);
            case "labAdmin" -> ReservationActor.laboratoryAdministrator(userId, name);
            case "teacher" -> ReservationActor.teacher(userId, name);
            default -> throw new IllegalArgumentException("无权查询预约数据");
        };
    }
}
