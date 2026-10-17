/*
 * 实验室预约数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.entity;

/** 预约状态转换规则；仅返回目标状态，不持久化，也不代替操作者权限与实时冲突校验。 */
public final class ReservationStateMachine {
    /**
     * 构造 ReservationStateMachine，初始化当前实例。
     */
    private ReservationStateMachine() {
    }

    /** 有冲突的申请仍建立 CONFLICT 记录，不能直接进入待通过状态。 */
    public static ReservationStatus initialStatus(boolean hasConflict) {
        return hasConflict ? ReservationStatus.CONFLICT : ReservationStatus.PENDING;
    }

    /** 只允许 PENDING → APPROVED；实际审核服务须先重新检查可用性。 */
    public static ReservationStatus approve(ReservationStatus status) {
        return transition(status, ReservationStatus.APPROVED);
    }

    /** 允许待审核和冲突申请被驳回，已通过等其他状态不能重复审核。 */
    public static ReservationStatus reject(ReservationStatus status) {
        return transition(status, ReservationStatus.REJECTED);
    }

    /** 这里只允许待审核申请过期；已通过记录的后续生命周期不由此方法定义。 */
    public static ReservationStatus expire(ReservationStatus status) {
        return transition(status, ReservationStatus.EXPIRED);
    }

    /**
     * 按白名单验证一次状态转换，不接受任意枚举值之间的跳转。
     * @param current 数据库回读的当前状态，不能信任客户端自报的旧状态
     * @param target 目标状态
     * @return 转换允许时返回 target，不修改任何外部对象
     * @throws IllegalArgumentException 任一状态为空
     * @throws IllegalStateException 当前/目标组合未在白名单声明
     */
    public static ReservationStatus transition(ReservationStatus current, ReservationStatus target) {
        if (current == null || target == null) {
            throw new IllegalArgumentException("Reservation status transition requires current and target statuses");
        }
        if (current == ReservationStatus.PENDING
                && (target == ReservationStatus.APPROVED
                || target == ReservationStatus.REJECTED
                || target == ReservationStatus.EXPIRED)) {
            return target;
        }
        if (current == ReservationStatus.CONFLICT && target == ReservationStatus.REJECTED) {
            return target;
        }
        throw new IllegalStateException("Unsupported reservation status transition: "
                + current.storageValue() + " -> " + target.storageValue());
    }
}
