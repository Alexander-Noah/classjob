/*
 * 实验室预约回归测试；测试夹具与生产配置分离。
 * 覆盖场景：propagatesUnavailableLaboratoryMessage、mapsAvailableLaboratorySnapshot、rejectsDuplicateManagedLaboratoryIds、rejectsDuplicateLaboratorySummaryIds 等。
 */
package org.example.backend.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationLabServiceTest {
    private static final TimeRange RANGE = new TimeRange(
            Instant.parse("2026-10-12T01:00:00Z"),
            Instant.parse("2026-10-12T03:00:00Z")
    );

    /**
     * propagatesUnavailableLaboratory消息的处理逻辑。
     */
    @Test
    void propagatesUnavailableLaboratoryMessage() {
        var service = service(List.of(), /** 处理 service 调用中的回调逻辑。 入参：labId、capacity、startsAt、endsAt。 */ (labId, capacity, startsAt, endsAt) ->
                CapabilityResult.unavailable("laboratory data is unavailable"));

        CapabilityResult<LabAvailabilitySnapshot> result = service.checkAvailability(7L, 30, RANGE);

        assertEquals(CapabilityStatus.UNAVAILABLE, result.status());
        assertEquals("laboratory data is unavailable", result.message());
    }

    /**
     * 映射可用Laboratory快照的处理逻辑。
     */
    @Test
    void mapsAvailableLaboratorySnapshot() {
        var service = service(List.of(), /** 处理 service 调用中的回调逻辑。 入参：labId、capacity、startsAt、endsAt。 */ (labId, capacity, startsAt, endsAt) ->
                CapabilityResult.available(new LabAvailabilityView(labId, 48, true)));

        CapabilityResult<LabAvailabilitySnapshot> result = service.checkAvailability(7L, 30, RANGE);

        assertEquals(CapabilityResult.available(new LabAvailabilitySnapshot(7L, 48, true)), result);
    }

    /**
     * rejectsDuplicateManagedLaboratory标识集合的处理逻辑。
     */
    @Test
    void rejectsDuplicateManagedLaboratoryIds() {
        var service = service(List.of(lab(7L, "LAB-7", "Lab Seven"), lab(7L, "OTHER", "Other")), availableLab());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                /**
                 * 处理 assertThrows 调用中的回调逻辑。
                 */
                () -> service.findManagedLaboratoryIds(101L)
        );

        assertEquals("lab catalog returned a duplicate laboratory id", exception.getMessage());
    }

    /**
     * rejectsDuplicateLaboratory摘要标识集合的处理逻辑。
     */
    @Test
    void rejectsDuplicateLaboratorySummaryIds() {
        var service = service(List.of(lab(7L, "LAB-7", "Lab Seven"), lab(7L, "OTHER", "Other")), availableLab());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                /**
                 * 处理 assertThrows 调用中的回调逻辑。
                 */
                () -> service.findLaboratorySummaries(Set.of(7L))
        );

        assertEquals("lab catalog returned a duplicate laboratory id", exception.getMessage());
    }

    /**
     * rejectsNullAndInvalidLaboratory标识集合WithExisting消息的处理逻辑。
     */
    @Test
    void rejectsNullAndInvalidLaboratoryIdSetsWithExistingMessages() {
        var service = service(List.of(), availableLab());
        Set<Long> idsWithNull = new HashSet<>();
        idsWithNull.add(null);

        IllegalArgumentException missing = assertThrows(
                IllegalArgumentException.class,
                /**
                 * 处理 assertThrows 调用中的回调逻辑。
                 */
                () -> service.findLaboratorySummaries(null)
        );
        IllegalArgumentException nullId = assertThrows(
                IllegalArgumentException.class,
                /**
                 * 处理 assertThrows 调用中的回调逻辑。
                 */
                () -> service.findLaboratorySummaries(idsWithNull)
        );
        IllegalArgumentException invalid = assertThrows(
                IllegalArgumentException.class,
                /**
                 * 处理 assertThrows 调用中的回调逻辑。
                 */
                () -> service.findLaboratorySummaries(Set.of(0L))
        );

        assertEquals("laboratory ids are required", missing.getMessage());
        assertEquals("laboratory ids must be positive", nullId.getMessage());
        assertEquals("laboratory ids must be positive", invalid.getMessage());
    }

    /**
     * returnsRequestedLaboratory摘要Only的处理逻辑。
     */
    @Test
    void returnsRequestedLaboratorySummariesOnly() {
        var service = service(
                List.of(lab(7L, "LAB-7", "Lab Seven"), lab(8L, "LAB-8", "Lab Eight")),
                availableLab()
        );

        Map<Long, LaboratorySummary> summaries = service.findLaboratorySummaries(Set.of(8L));

        assertEquals(Map.of(8L, new LaboratorySummary("Lab Eight", "LAB-8")), summaries);
    }

    /**
     * 服务的处理逻辑。
     * @param labs 实验室参数。
     * @param availability availability参数。
     * @return ReservationLabService 类型的处理结果。
     */
    private static ReservationLabService service(List<LabView> labs, LabAvailabilityQuery availability) {
        return new ReservationLabService(new StubCatalog(labs), availability);
    }

    /**
     * 可用实验室的处理逻辑。
     * @return LabAvailabilityQuery 类型的处理结果。
     */
    private static LabAvailabilityQuery availableLab() {
        return /** 处理当前调用传入的回调逻辑。 入参：labId、capacity、startsAt、endsAt。 */ (labId, capacity, startsAt, endsAt) ->
                CapabilityResult.available(new LabAvailabilityView(labId, capacity, true));
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

    private record StubCatalog(List<LabView> labs) implements LabCatalogQuery {
        /**
         * 列出当前操作。
         * @param scope scope参数。
         * @return 当前操作得到的记录集合。
         */
        @Override
        public List<LabView> list(LabAccessScope scope) {
            return labs;
        }

        /**
         * 查找By标识。
         * @param id 标识参数。
         * @param scope scope参数。
         * @return LabView 类型的处理结果。
         */
        @Override
        public LabView findById(long id, LabAccessScope scope) {
            return labs.stream().filter(/** 判断当前条目是否符合筛选条件。 入参：lab。 */ lab -> Long.valueOf(id).equals(lab.id())).findFirst().orElse(null);
        }
    }
}
