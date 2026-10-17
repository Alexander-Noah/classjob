/*
 * 实验室预约能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.service.ReservationActor;
import org.example.backend.service.ReservationView;

public interface ReservationCommandHandler {
    /**
     * 创建当前操作。
     * @param command 命令参数。
     * @param actor 操作者参数。
     * @return ReservationView 类型的处理结果。
     */
    ReservationView create(CreateReservationCommand command, ReservationActor actor);

    /**
     * 批准当前操作。
     * @param command 命令参数。
     * @param actor 操作者参数。
     * @return ReservationView 类型的处理结果。
     */
    ReservationView approve(ReviewReservationCommand command, ReservationActor actor);

    /**
     * 拒绝当前操作。
     * @param command 命令参数。
     * @param actor 操作者参数。
     * @return ReservationView 类型的处理结果。
     */
    ReservationView reject(ReviewReservationCommand command, ReservationActor actor);
}
