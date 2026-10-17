/*
 * 查询能力状态数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

/**
 * 查询能力的显式结果，不把外部系统不可用伪装成空集合或业务上的“不存在”。
 *
 * @param <T> 成功载荷类型
 * @param status 能力是否可用，调用方应先判断状态再消费 value
 * @param value 成功载荷；工厂方法不限制 null，要求非空的调用方仍需检查
 * @param errorCode 失败分类；available 工厂方法将其置为 null
 * @param message 失败原因，可用于诊断，不应包含凭据或其他秘密
 */
public record CapabilityResult<T>(
        CapabilityStatus status,
        T value,
        String errorCode,
        String message
) {

    /** 创建可用结果，不校验载荷的业务完整性，也不执行 I/O。 */
    public static <T> CapabilityResult<T> available(T value) {
        return new CapabilityResult<>(CapabilityStatus.AVAILABLE, value, null, null);
    }

    /** 创建无载荷的不可用结果；保留原因，由上层决定阻断、重试或展示。 */
    public static <T> CapabilityResult<T> unavailable(String message) {
        return new CapabilityResult<>(
                CapabilityStatus.UNAVAILABLE,
                null,
                "CAPABILITY_UNAVAILABLE",
                message
        );
    }
}
