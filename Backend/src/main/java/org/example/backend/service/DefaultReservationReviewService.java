/*
 * 实验室预约实现模块。
 * 主要协作组件：ReservationPersistenceService、ReservationCatalogQuery。
 */
package org.example.backend.service;

import org.example.backend.entity.ReservationStateMachine;
import org.example.backend.entity.ReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;

@Service
@Transactional
public class DefaultReservationReviewService {
    private static final Set<ReservationStatus> APPROVABLE = Set.of(ReservationStatus.PENDING);
    private static final Set<ReservationStatus> REJECTABLE = Set.of(ReservationStatus.PENDING, ReservationStatus.CONFLICT);

    private final ReservationPersistenceService reservations;
    private final ReservationCatalogQuery catalog;
    private final ReservationAvailabilityEvaluator availability;

    /**
     * 构造 DefaultReservationReviewService，初始化当前实例。
     * @param reservations 预约参数。
     * @param catalog catalog参数。
     * @param availability availability参数。
     */
    public DefaultReservationReviewService(ReservationPersistenceService reservations, ReservationCatalogQuery catalog,
                                           ReservationAvailabilityEvaluator availability) {
        this.reservations = Objects.requireNonNull(reservations, "reservations");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.availability = Objects.requireNonNull(availability, "availability");
    }

    /**
     * 审核通过前重新查询实时可用性，排除当前预约自身；不能仅沿用提交时的冲突标记。
     *
     * @param command 目标预约及可选审核备注
     * @param actor 已认证的系统管理员或实验室管理员，教师不能审核
     * @return 条件更新后重新回读并核对的记录
     * @throws IllegalArgumentException 标识无效或记录不在操作者可见范围
     * @throws IllegalStateException 当前状态不可审核、发生冲突、并发修改或回读校验失败
     */
    public ReservationView approve(ReviewReservationCommand command, ReservationActor actor) {
        return review(command, actor, ReservationStatus.APPROVED, APPROVABLE, "通过");
    }

    /** 驳回待审核或冲突状态记录；沿用审核身份、条件更新与回读校验，不执行通过前的可用性检查。 */
    public ReservationView reject(ReviewReservationCommand command, ReservationActor actor) {
        return review(command, actor, ReservationStatus.REJECTED, REJECTABLE, "驳回");
    }

    /**
     * review的处理逻辑。
     * @param command 命令参数。
     * @param actor 操作者参数。
     * @param target 目标参数。
     * @param allowedCurrentStatuses allowed当前Statuses参数。
     * @param defaultRemark 默认Remark参数。
     * @return ReservationView 类型的处理结果。
     */
    private ReservationView review(ReviewReservationCommand command, ReservationActor actor, ReservationStatus target,
                                   Set<ReservationStatus> allowedCurrentStatuses, String defaultRemark) {
        if (command == null || command.reservationId() == null || command.reservationId() <= 0) {
            throw new IllegalArgumentException("Review command requires a positive reservation id");
        }
        ReservationActor authenticated = Objects.requireNonNull(actor, "actor");
        String reviewerName = requiredReviewerName(authenticated);
        assertCanReview(authenticated);
        ReservationAccessScope scope = ReservationAccessScope.forActor(authenticated);
        ReservationView current = catalog.findById(command.reservationId(), scope);
        if (current == null) {
            throw new IllegalArgumentException("Reservation is not visible to the reviewer");
        }
        if (!allowedCurrentStatuses.contains(current.status())) {
            throw new IllegalStateException("Reservation is not in a reviewable status");
        }
        if (target == ReservationStatus.APPROVED) {
            assertCurrentlyAvailable(current);
        }
        transition(current.status(), target);
        String remark = normalizedRemark(command.reviewRemark(), defaultRemark);
        ReservationReviewUpdate update = new ReservationReviewUpdate(command.reservationId(), target, false, reviewerName,
                remark, allowedCurrentStatuses);
        // SQL 的旧状态条件是本次更新的并发检查，不依赖前端按钮是否已禁用。
        if (!reservations.updateReview(update)) {
            throw new IllegalStateException("Reservation review failed because the state is stale or changed concurrently");
        }
        ReservationView reviewed = catalog.findById(command.reservationId(), scope);
        if (reviewed == null || reviewed.status() != target || reviewed.conflictFlag()
                || !reviewerName.equals(reviewed.reviewerName()) || !remark.equals(reviewed.reviewRemark())) {
            throw new IllegalStateException("Reservation review result could not be authoritatively verified");
        }
        return reviewed;
    }

    /**
     * 断言Currently可用。
     * @param current 当前参数。
     */
    private void assertCurrentlyAvailable(ReservationView current) {
        ReservationTimeRangeParser.ParsedReservationTimeRange parsed;
        try {
            parsed = ReservationTimeRangeParser.parse(current.timeRange(), current.reservationDate());
            if (availability.hasConflict(new ReservationAvailabilityRequest(current.labId(), current.reservationDate(),
                    parsed.normalized(), parsed.timeRange(), current.attendeeCount(), current.id()))) {
                throw new IllegalStateException("Reservation can no longer be approved because it conflicts with current availability");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Reservation contains invalid availability data", exception);
        }
    }

    /**
     * requiredReviewer名称的处理逻辑。
     * @param actor 操作者参数。
     * @return String 类型的处理结果。
     */
    private String requiredReviewerName(ReservationActor actor) {
        if (actor.displayName() == null || actor.displayName().isBlank()) {
            throw new IllegalStateException("Authenticated reviewer display name is required");
        }
        return actor.displayName();
    }

    /**
     * 断言CanReview。
     * @param actor 操作者参数。
     */
    private void assertCanReview(ReservationActor actor) {
        if (actor.role() == ReservationActorRole.SYSTEM_ADMINISTRATOR
                || actor.role() == ReservationActorRole.LABORATORY_ADMINISTRATOR) {
            return;
        }
        if (actor.role() == ReservationActorRole.TEACHER) {
            throw new IllegalStateException("Teachers cannot review reservations");
        }
        throw new IllegalStateException("Unsupported reservation actor role: " + actor.role());
    }

    /**
     * transition的处理逻辑。
     * @param current 当前参数。
     * @param target 目标参数。
     */
    private void transition(ReservationStatus current, ReservationStatus target) {
        if (target == ReservationStatus.APPROVED) {
            ReservationStateMachine.approve(current);
        } else if (target == ReservationStatus.REJECTED) {
            ReservationStateMachine.reject(current);
        } else {
            throw new IllegalStateException("Unsupported review target: " + target);
        }
    }

    /**
     * 规范化结果Remark的处理逻辑。
     * @param remark remark参数。
     * @param defaultRemark 默认Remark参数。
     * @return String 类型的处理结果。
     */
    private String normalizedRemark(String remark, String defaultRemark) {
        return remark == null || remark.isBlank() ? defaultRemark : remark.trim();
    }
}
