/*
 * 实验室预约实现模块。
 * 主要协作组件：ReservationPersistenceService、ReservationCatalogQuery、ReservationLabService。
 */
package org.example.backend.service;

import org.example.backend.entity.ReservationStateMachine;
import org.example.backend.entity.ReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

@Service
// 通过 Spring Bean 的公开方法进入时，校验、重叠查询、写入与回读处于同一串行化事务。
// 数据库仍可能返回死锁/串行化失败；本服务没有隐藏的自动重试或请求去重机制。
@Transactional(isolation = Isolation.SERIALIZABLE)
public class DefaultReservationCreationService {
    private final ReservationPersistenceService reservations;
    private final ReservationCatalogQuery catalog;
    private final ReservationLabService laboratories;
    private final ReservationAvailabilityEvaluator availability;

    /**
     * 构造 DefaultReservationCreationService，初始化当前实例。
     * @param reservations 预约参数。
     * @param catalog catalog参数。
     * @param laboratories laboratories参数。
     * @param availability availability参数。
     */
    public DefaultReservationCreationService(ReservationPersistenceService reservations, ReservationCatalogQuery catalog,
                                             ReservationLabService laboratories,
                                             ReservationAvailabilityEvaluator availability) {
        this.reservations = Objects.requireNonNull(reservations, "reservations");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.laboratories = Objects.requireNonNull(laboratories, "laboratories");
        this.availability = Objects.requireNonNull(availability, "availability");
    }

    /**
     * 校验申请人数据和实验室访问范围，评估可用性后写入并回读服务端记录。
     * 发现冲突时由状态机创建冲突状态记录，而不是把请求当作成功占用。
     *
     * @param command 预约字段，必须包含有效实验室、人数、日期和同日时间区间
     * @param actor 由认证上下文构造的操作者，不能直接信任请求体声明的角色
     * @return 操作者可见范围内的已落库记录，不回显提交对象代替数据库结果
     * @throws IllegalArgumentException 参数无效或实验室管理员操作超出管理范围
     * @throws IllegalStateException 查询能力不可用、角色不受支持或写入结果无法回读
     * @throws NullPointerException actor 为 null
     */
    public ReservationView create(CreateReservationCommand command, ReservationActor actor) {
        ValidatedCommand validated = validate(command);
        ReservationActor authenticatedActor = Objects.requireNonNull(actor, "actor");
        assertCanCreateForLaboratory(authenticatedActor, validated.labId());
        boolean conflict = availability.hasConflict(new ReservationAvailabilityRequest(validated.labId(), validated.reservationDate(),
                validated.timeRange().normalized(), validated.timeRange().timeRange(), validated.attendeeCount(), null));
        ReservationStatus status = ReservationStateMachine.initialStatus(conflict);
        long reservationId = reservations.insert(new NewReservationRecord(authenticatedActor.userId(), validated.applicantName(),
                validated.department(), validated.contact(), validated.labId(), validated.reservationDate(), validated.timeRange().normalized(),
                validated.scene(), validated.attendeeCount(), validated.reason(), status, conflict, "待审核", null));
        ReservationView created = catalog.findById(reservationId, ReservationAccessScope.forActor(authenticatedActor));
        if (created == null) {
            throw new IllegalStateException("Inserted reservation cannot be read within actor scope");
        }
        return created;
    }

    /**
     * 断言CanCreateForLaboratory。
     * @param actor 操作者参数。
     * @param labId 实验室标识参数。
     */
    private void assertCanCreateForLaboratory(ReservationActor actor, long labId) {
        if (actor.role() == ReservationActorRole.SYSTEM_ADMINISTRATOR || actor.role() == ReservationActorRole.TEACHER) {
            return;
        }
        if (actor.role() != ReservationActorRole.LABORATORY_ADMINISTRATOR) {
            throw new IllegalStateException("Unsupported reservation actor role: " + actor.role());
        }
        Set<Long> managed = laboratories.findManagedLaboratoryIds(actor.userId());
        if (managed == null) {
            throw new IllegalStateException("Managed laboratory capability did not return a set");
        }
        if (!managed.contains(labId)) {
            throw new IllegalArgumentException("Laboratory administrator cannot create for this laboratory");
        }
    }

    /**
     * 校验当前操作。
     * @param command 命令参数。
     * @return ValidatedCommand 类型的处理结果。
     */
    private ValidatedCommand validate(CreateReservationCommand command) {
        if (command == null || command.labId() == null || command.labId() <= 0 || command.attendeeCount() == null
                || command.attendeeCount() <= 0 || command.reservationDate() == null) {
            throw new IllegalArgumentException("Reservation command has invalid required fields");
        }
        ReservationTimeRangeParser.ParsedReservationTimeRange timeRange = ReservationTimeRangeParser.parse(command.timeRange(), command.reservationDate());
        return new ValidatedCommand(required(command.applicantName(), "applicant name"), required(command.department(), "department"),
                required(command.contact(), "contact"), command.labId(), command.reservationDate(), timeRange,
                required(command.scene(), "scene"), command.attendeeCount(), required(command.reason(), "reason"));
    }

    /**
     * required的处理逻辑。
     * @param value 值参数。
     * @param field field参数。
     * @return String 类型的处理结果。
     */
    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Reservation " + field + " is required");
        }
        return value.trim();
    }

    private record ValidatedCommand(String applicantName, String department, String contact, Long labId, LocalDate reservationDate,
                                    ReservationTimeRangeParser.ParsedReservationTimeRange timeRange, String scene,
                                    Integer attendeeCount, String reason) { }
}
