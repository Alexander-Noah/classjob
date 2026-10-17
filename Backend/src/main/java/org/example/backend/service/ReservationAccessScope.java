/*
 * 实验室预约数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import java.util.Objects;

public record ReservationAccessScope(ReservationActor actor) {
    public ReservationAccessScope {
        Objects.requireNonNull(actor, "Reservation access scope requires an actor");
    }

    public static ReservationAccessScope forActor(ReservationActor actor) {
        return new ReservationAccessScope(actor);
    }
}
