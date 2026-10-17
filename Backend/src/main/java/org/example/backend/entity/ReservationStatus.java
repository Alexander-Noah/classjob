/*
 * 实验室预约数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.entity;

import java.util.Arrays;

public enum ReservationStatus {
    PENDING("待审核", false),
    CONFLICT("冲突", false),
    APPROVED("已通过", true),
    REJECTED("已驳回", true),
    EXPIRED("已过期", true),
    COUNTERSIGNING("加签中", false),
    COMPLETED("已完成", true),
    CANCELLED("已取消", true);

    /** 历史数据库保存的中文值，不能直接改成 enum.name()，否则旧记录无法回读。 */
    private final String storageValue;
    /** 工作流终态标记；不等同于“无需参与占用查询”，已通过预约仍可能占用时间。 */
    private final boolean finalStatus;

    ReservationStatus(String storageValue, boolean finalStatus) {
        this.storageValue = storageValue;
        this.finalStatus = finalStatus;
    }

    public String storageValue() {
        return storageValue;
    }

    public boolean isFinal() {
        return finalStatus;
    }

    /**
     * 严格反序列化数据库状态，不将未知取值默认为待审核或已通过。
     * @param value 与 storageValue 完全一致的文本，本方法不执行 trim 或别名兼容
     * @return 匹配的状态枚举
     * @throws IllegalArgumentException 值为空或尚未支持
     */
    public static ReservationStatus fromStorageValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Reservation status is required");
        }
        return Arrays.stream(values())
                .filter(status -> status.storageValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported reservation status: " + value));
    }
}
