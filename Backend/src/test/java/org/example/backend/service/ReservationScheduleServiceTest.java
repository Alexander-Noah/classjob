/*
 * 课表与时间占用回归测试；测试夹具与生产配置分离。
 * 覆盖场景：passesTrimmedLaboratoryCodeAndNameAliasesToTeachingSchedule、propagatesUnavailableTeachingScheduleMessage、malformedReservationRangeUsesExistingUnavailableMessage、crossDateReservationRangeUsesExistingUnavailableMessage。
 */
package org.example.backend.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservationScheduleServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 12);
    private static final TimeRange SAME_DATE_RANGE = new TimeRange(
            Instant.parse("2026-10-12T01:00:00Z"),
            Instant.parse("2026-10-12T03:00:00Z")
    );

    /**
     * passesTrimmedLaboratory编码And名称Aliases目标Teaching课表的处理逻辑。
     */
    @Test
    void passesTrimmedLaboratoryCodeAndNameAliasesToTeachingSchedule() {
        AtomicReference<ScheduleOccupancyRequest> captured = new AtomicReference<>();
        ScheduleOccupancyQuery schedules = /** 处理当前调用传入的回调逻辑。 入参：request。 */ request -> {
            captured.set(request);
            return CapabilityResult.available(new ScheduleOccupancyView(true));
        };
        var service = new ReservationScheduleService(schedules, catalog(lab(7L, " LAB-7 ", " Lab Seven ")));

        CapabilityResult<ScheduleConflictSnapshot> result = service.checkConflict(7L, DATE, SAME_DATE_RANGE);

        assertEquals(Set.of("LAB-7", "Lab Seven"), captured.get().laboratoryAliases());
        assertEquals(CapabilityResult.available(new ScheduleConflictSnapshot(7L, DATE, SAME_DATE_RANGE, true)), result);
    }

    /**
     * propagatesUnavailableTeaching课表消息的处理逻辑。
     */
    @Test
    void propagatesUnavailableTeachingScheduleMessage() {
        var service = new ReservationScheduleService(
                /**
                 * 处理当前调用传入的回调逻辑。
                 * 入参：request。
                 */
                request -> CapabilityResult.unavailable("teaching schedule provider is unavailable"),
                catalog(lab(7L, "LAB-7", "Lab Seven"))
        );

        CapabilityResult<ScheduleConflictSnapshot> result = service.checkConflict(7L, DATE, SAME_DATE_RANGE);

        assertEquals(CapabilityStatus.UNAVAILABLE, result.status());
        assertEquals("teaching schedule provider is unavailable", result.message());
    }

    /**
     * malformed预约范围UsesExistingUnavailable消息的处理逻辑。
     */
    @Test
    void malformedReservationRangeUsesExistingUnavailableMessage() {
        var service = new ReservationScheduleService(
                /**
                 * 处理当前调用传入的回调逻辑。
                 * 入参：request。
                 */
                request -> CapabilityResult.available(new ScheduleOccupancyView(false)),
                catalog(lab(7L, "LAB-7", "Lab Seven"))
        );
        TimeRange malformed = new TimeRange(SAME_DATE_RANGE.endsAt(), SAME_DATE_RANGE.startsAt());

        CapabilityResult<ScheduleConflictSnapshot> result = service.checkConflict(7L, DATE, malformed);

        assertEquals(CapabilityStatus.UNAVAILABLE, result.status());
        assertEquals("reservation request is malformed", result.message());
    }

    /**
     * cross日期预约范围UsesExistingUnavailable消息的处理逻辑。
     */
    @Test
    void crossDateReservationRangeUsesExistingUnavailableMessage() {
        var service = new ReservationScheduleService(
                /**
                 * 处理当前调用传入的回调逻辑。
                 * 入参：request。
                 */
                request -> CapabilityResult.available(new ScheduleOccupancyView(false)),
                catalog(lab(7L, "LAB-7", "Lab Seven"))
        );
        TimeRange crossDate = new TimeRange(
                Instant.parse("2026-10-12T15:30:00Z"),
                Instant.parse("2026-10-12T17:30:00Z")
        );

        CapabilityResult<ScheduleConflictSnapshot> result = service.checkConflict(7L, DATE, crossDate);

        assertEquals(CapabilityStatus.UNAVAILABLE, result.status());
        assertEquals("reservation time must be within reservation date", result.message());
    }

    /**
     * catalog的处理逻辑。
     * @param lab 实验室参数。
     * @return LabCatalogQuery 类型的处理结果。
     */
    private static LabCatalogQuery catalog(LabView lab) {
        return new LabCatalogQuery() {
            /**
             * 列出当前操作。
             * @param scope scope参数。
             * @return 当前操作得到的记录集合。
             */
            @Override
            public List<LabView> list(LabAccessScope scope) {
                return List.of(lab);
            }

            /**
             * 查找By标识。
             * @param id 标识参数。
             * @param scope scope参数。
             * @return LabView 类型的处理结果。
             */
            @Override
            public LabView findById(long id, LabAccessScope scope) {
                return lab;
            }
        };
    }

    /**
     * 实验室的处理逻辑。
     * @param id 标识参数。
     * @param code 编码参数。
     * @param name 名称参数。
     * @return LabView 类型的处理结果。
     */
    private static LabView lab(Long id, String code, String name) {
        return new LabView(id, code, name, null, null, null, null, 50, null, null, null, 0, null, null, null);
    }
}
