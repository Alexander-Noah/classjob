/*
 * 实验室预约数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import java.util.Objects;

public record ReservationAccessScope(ReservationActor actor) {
    /**
     * 构造 ReservationAccessScope，初始化当前实例。
     */
    public ReservationAccessScope {
        Objects.requireNonNull(actor, "Reservation access scope requires an actor");
    }

    /**
     * for操作者的处理逻辑。
     * @param actor 操作者参数。
     * @return ReservationAccessScope 类型的处理结果。
     */
    public static ReservationAccessScope forActor(ReservationActor actor) {
        return new ReservationAccessScope(actor);
    }
}
