package org.example.backend.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 按实验室编码与名称查询教学占用，使用上海时区解释预约时间。
 * 数据源不可用时返回 UNAVAILABLE，不能把查询失败误认为“没有冲突”。
 */
@Service
public class ReservationScheduleService {
    private static final ZoneId RESERVATION_TIME_ZONE = ZoneId.of("Asia/Shanghai");

    private final ScheduleOccupancyQuery schedules;
    private final LabCatalogQuery laboratories;

    /**
     * 构造 ReservationScheduleService，初始化当前实例。
     * @param schedules 课表参数。
     * @param laboratories laboratories参数。
     */
    public ReservationScheduleService(ScheduleOccupancyQuery schedules, LabCatalogQuery laboratories) {
        this.schedules = schedules;
        this.laboratories = laboratories;
    }

    /** 只接受起止时间均落在预约日期内的有效区间，查询结果同时携带能力可用状态。 */
    public CapabilityResult<ScheduleConflictSnapshot> checkConflict(Long labId, LocalDate date, TimeRange range) {
        if (labId == null || labId <= 0 || date == null || !valid(range)) {
            return unavailable("reservation request is malformed");
        }
        LocalDateTime start = LocalDateTime.ofInstant(range.startsAt(), RESERVATION_TIME_ZONE);
        LocalDateTime end = LocalDateTime.ofInstant(range.endsAt(), RESERVATION_TIME_ZONE);
        if (!start.toLocalDate().equals(date) || !end.toLocalDate().equals(date)) {
            return unavailable("reservation time must be within reservation date");
        }
        LabView lab;
        try {
            lab = laboratories.findById(labId, LabAccessScope.all());
        } catch (RuntimeException exception) {
            return unavailable("laboratory context is unavailable");
        }
        if (lab == null || !labId.equals(lab.id())) {
            return unavailable("laboratory context is unavailable");
        }
        Set<String> aliases = aliases(lab);
        if (aliases.isEmpty()) {
            return unavailable("laboratory identity is unavailable");
        }
        try {
            var result = schedules.check(new ScheduleOccupancyRequest(date, start.toLocalTime(), end.toLocalTime(), aliases));
            if (result == null || result.status() != CapabilityStatus.AVAILABLE || result.value() == null) {
                return unavailable(result == null || result.message() == null
                        ? "teaching schedule is unavailable" : result.message());
            }
            return CapabilityResult.available(new ScheduleConflictSnapshot(labId, date, range, result.value().occupied()));
        } catch (RuntimeException exception) {
            return unavailable("teaching schedule is unavailable");
        }
    }

    /**
     * aliases的处理逻辑。
     * @param lab 实验室参数。
     * @return 当前操作得到的记录集合。
     */
    private Set<String> aliases(LabView lab) {
        Set<String> aliases = new LinkedHashSet<>();
        if (text(lab.labCode())) {
            aliases.add(lab.labCode().trim());
        }
        if (text(lab.labName())) {
            aliases.add(lab.labName().trim());
        }
        return Set.copyOf(aliases);
    }

    /**
     * 有效的处理逻辑。
     * @param range 范围参数。
     * @return 当前判断的布尔结果。
     */
    private boolean valid(TimeRange range) {
        return range != null && range.startsAt() != null && range.endsAt() != null
                && range.startsAt().isBefore(range.endsAt());
    }

    /**
     * 文本的处理逻辑。
     * @param value 值参数。
     * @return 当前判断的布尔结果。
     */
    private boolean text(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * unavailable的处理逻辑。
     * @param message 消息参数。
     * @return CapabilityResult<ScheduleConflictSnapshot> 类型的处理结果。
     */
    private CapabilityResult<ScheduleConflictSnapshot> unavailable(String message) {
        return CapabilityResult.unavailable(message);
    }
}
