/*
 * 实验室预约实现模块。
 * 主要协作组件：DefaultReservationCreationService、DefaultReservationReviewService。
 */
package org.example.backend.service;

import org.example.backend.service.ReservationActor;
import org.example.backend.service.ReservationView;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DefaultReservationCommandHandler implements ReservationCommandHandler {
    private static final int MAX_CREATION_ATTEMPTS = 3;
    private final DefaultReservationCreationService creation;
    private final DefaultReservationReviewService review;

    /**
     * 构造 DefaultReservationCommandHandler，初始化当前实例。
     * @param creation creation参数。
     * @param review review参数。
     */
    public DefaultReservationCommandHandler(DefaultReservationCreationService creation, DefaultReservationReviewService review) {
        this.creation = Objects.requireNonNull(creation, "creation");
        this.review = Objects.requireNonNull(review, "review");
    }

    /**
     * 创建当前操作。
     * @param command 命令参数。
     * @param actor 操作者参数。
     * @return ReservationView 类型的处理结果。
     */
    @Override
    public ReservationView create(CreateReservationCommand command, ReservationActor actor) {
        for (int attempt = 1; attempt <= MAX_CREATION_ATTEMPTS; attempt++) {
            try {
                return creation.create(command, actor);
            } catch (PessimisticLockingFailureException exception) {
                if (attempt == MAX_CREATION_ATTEMPTS) {
                    throw exception;
                }
            }
        }
        throw new IllegalStateException("reservation creation retry loop exhausted");
    }

    /**
     * 批准当前操作。
     * @param command 命令参数。
     * @param actor 操作者参数。
     * @return ReservationView 类型的处理结果。
     */
    @Override
    public ReservationView approve(ReviewReservationCommand command, ReservationActor actor) {
        return review.approve(command, actor);
    }

    /**
     * 拒绝当前操作。
     * @param command 命令参数。
     * @param actor 操作者参数。
     * @return ReservationView 类型的处理结果。
     */
    @Override
    public ReservationView reject(ReviewReservationCommand command, ReservationActor actor) {
        return review.reject(command, actor);
    }
}
