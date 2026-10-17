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

    /**
     * 构造 ReservationStatus，初始化当前实例。
     * @param storageValue storage值参数。
     * @param finalStatus final状态参数。
     */
    ReservationStatus(String storageValue, boolean finalStatus) {
        this.storageValue = storageValue;
        this.finalStatus = finalStatus;
    }

    /**
     * storage值的处理逻辑。
     * @return String 类型的处理结果。
     */
    public String storageValue() {
        return storageValue;
    }

    /**
     * 判断是否为Final。
     * @return 当前判断的布尔结果。
     */
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
                .filter(/** 判断当前条目是否符合筛选条件。 入参：status。 */ status -> status.storageValue.equals(value))
                .findFirst()
                .orElseThrow(/** 处理 orElseThrow 调用中的回调逻辑。 */ () -> new IllegalArgumentException("Unsupported reservation status: " + value));
    }
}
