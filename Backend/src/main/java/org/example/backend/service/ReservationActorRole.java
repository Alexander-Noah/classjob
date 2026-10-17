/*
 * 实验室预约的枚举取值；新增状态时同步检查序列化、分支处理与持久化映射。
 */
package org.example.backend.service;

public enum ReservationActorRole {
    SYSTEM_ADMINISTRATOR,
    LABORATORY_ADMINISTRATOR,
    TEACHER
}
