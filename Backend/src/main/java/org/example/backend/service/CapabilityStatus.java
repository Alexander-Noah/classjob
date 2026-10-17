/*
 * 查询能力状态的枚举取值；新增状态时同步检查序列化、分支处理与持久化映射。
 */
package org.example.backend.service;

public enum CapabilityStatus {
    AVAILABLE,
    UNAVAILABLE
}
