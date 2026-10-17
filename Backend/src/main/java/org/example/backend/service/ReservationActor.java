/*
 * 实验室预约数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import java.util.Objects;

public record ReservationActor(Long userId, ReservationActorRole role, String displayName) {
    /**
     * 构造 ReservationActor，初始化当前实例。
     * @param userId 用户标识参数。
     * @param role 角色参数。
     */
    public ReservationActor(Long userId, ReservationActorRole role) {
        this(userId, role, null);
    }

    /**
     * 构造 ReservationActor，初始化当前实例。
     */
    public ReservationActor {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Authenticated user id must be positive");
        }
        Objects.requireNonNull(role, "Authenticated actor role is required");
        displayName = displayName == null ? null : displayName.trim();
    }

    /**
     * systemAdministrator的处理逻辑。
     * @param userId 用户标识参数。
     * @return ReservationActor 类型的处理结果。
     */
    public static ReservationActor systemAdministrator(long userId) {
        return systemAdministrator(userId, null);
    }

    /**
     * systemAdministrator的处理逻辑。
     * @param userId 用户标识参数。
     * @param displayName display名称参数。
     * @return ReservationActor 类型的处理结果。
     */
    public static ReservationActor systemAdministrator(long userId, String displayName) {
        return new ReservationActor(userId, ReservationActorRole.SYSTEM_ADMINISTRATOR, displayName);
    }

    /**
     * laboratoryAdministrator的处理逻辑。
     * @param userId 用户标识参数。
     * @return ReservationActor 类型的处理结果。
     */
    public static ReservationActor laboratoryAdministrator(long userId) {
        return laboratoryAdministrator(userId, null);
    }

    /**
     * laboratoryAdministrator的处理逻辑。
     * @param userId 用户标识参数。
     * @param displayName display名称参数。
     * @return ReservationActor 类型的处理结果。
     */
    public static ReservationActor laboratoryAdministrator(long userId, String displayName) {
        return new ReservationActor(userId, ReservationActorRole.LABORATORY_ADMINISTRATOR, displayName);
    }

    /**
     * 教师的处理逻辑。
     * @param userId 用户标识参数。
     * @return ReservationActor 类型的处理结果。
     */
    public static ReservationActor teacher(long userId) {
        return teacher(userId, null);
    }

    /**
     * 教师的处理逻辑。
     * @param userId 用户标识参数。
     * @param displayName display名称参数。
     * @return ReservationActor 类型的处理结果。
     */
    public static ReservationActor teacher(long userId, String displayName) {
        return new ReservationActor(userId, ReservationActorRole.TEACHER, displayName);
    }
}
