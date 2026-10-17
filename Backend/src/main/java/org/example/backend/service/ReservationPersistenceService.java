/*
 * 实验室预约实现模块。
 * 主要协作组件：ReservationPersistenceMapper。
 */
package org.example.backend.service;

import org.example.backend.mapper.ReservationInsertRow;
import org.example.backend.mapper.ReservationOverlapCount;
import org.example.backend.mapper.ReservationPersistenceMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public final class ReservationPersistenceService {
    private final ReservationPersistenceMapper mapper;

    public ReservationPersistenceService(ReservationPersistenceMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public List<ReservationRecord> findAll(ReservationRecordScope scope) {
        List<ReservationRecord> records = mapper.findAll(Objects.requireNonNull(scope, "scope"));
        return records == null ? List.of() : records;
    }

    public Optional<ReservationRecord> findOne(long id, ReservationRecordScope scope) {
        return Optional.ofNullable(mapper.findOne(id, Objects.requireNonNull(scope, "scope")));
    }

    /**
     * 插入后读取 MyBatis 回填的数据库主键；事务由调用方业务服务管理。
     *
     * @param record 待落库字段，不携带客户端自选主键
     * @return 数据库生成的预约标识
     * @throws IllegalStateException SQL 执行后未回填主键
     * @throws NullPointerException record 为 null
     */
    public long insert(NewReservationRecord record) {
        ReservationInsertRow row = new ReservationInsertRow(Objects.requireNonNull(record, "record"));
        mapper.insert(row);
        if (row.getId() == null) {
            throw new IllegalStateException("Reservation insert did not return a generated ID");
        }
        return row.getId();
    }

    public long countActiveOverlaps(ReservationOverlapCheck check) {
        ReservationOverlapCount count = mapper.countActiveOverlaps(Objects.requireNonNull(check, "check"));
        if (count == null) {
            throw new IllegalStateException("Reservation overlap count was not returned");
        }
        return count.value();
    }

    /**
     * 以记录 ID 和允许的旧状态执行条件更新，避免覆盖已经发生的状态流转。
     *
     * @param update 目标状态、审核信息与允许的旧状态集合
     * @return 恰好更新一行时为 true；零行时由调用方判断状态已变化或记录不存在
     * @throws IllegalStateException 更新行数不是 0 或 1，说明持久化契约被破坏
     * @throws NullPointerException update 为 null
     */
    public boolean updateReview(ReservationReviewUpdate update) {
        int changed = mapper.updateReview(Objects.requireNonNull(update, "update"));
        if (changed == 0) {
            return false;
        }
        if (changed == 1) {
            return true;
        }
        throw new IllegalStateException("Conditional reservation review update changed " + changed + " rows");
    }
}
