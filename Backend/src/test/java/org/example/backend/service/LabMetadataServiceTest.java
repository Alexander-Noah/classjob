/*
 * 实验室元数据回归测试；测试夹具与生产配置分离。
 * 覆盖场景：convertsUnorderedRowsToIdentifierMaps、rejectsDuplicateRowsInsteadOfSilentlyOverwritingData、avoidsDatabaseCallsForEmptyIdentifierSets。
 */
package org.example.backend.service;

import org.example.backend.entity.DeviceCountRow;
import org.example.backend.entity.ManagerNameRow;
import org.example.backend.mapper.LabResourceMetadataMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LabMetadataServiceTest {
    /**
     * convertsUnordered行集合目标标识符映射的处理逻辑。
     */
    @Test
    void convertsUnorderedRowsToIdentifierMaps() {
        LabResourceMetadataMapper mapper = new StubMapper(
                List.of(new ManagerNameRow(202L, "Bob"), new ManagerNameRow(101L, "Alice")),
                List.of(new DeviceCountRow(2L, 22), new DeviceCountRow(1L, 11))
        );
        var adapter = new LabMetadataService(mapper);

        assertEquals("Alice", adapter.findNames(Set.of(101L, 202L)).get(101L));
        assertEquals("Bob", adapter.findNames(Set.of(101L, 202L)).get(202L));
        assertEquals(11, adapter.countByLabIds(Set.of(1L, 2L)).get(1L));
        assertEquals(22, adapter.countByLabIds(Set.of(1L, 2L)).get(2L));
    }

    /**
     * rejectsDuplicate行集合InsteadOfSilentlyOverwriting数据的处理逻辑。
     */
    @Test
    void rejectsDuplicateRowsInsteadOfSilentlyOverwritingData() {
        var managerAdapter = new LabMetadataService(new StubMapper(
                List.of(new ManagerNameRow(101L, "Alice"), new ManagerNameRow(101L, "Other")),
                List.of()
        ));
        var deviceAdapter = new LabMetadataService(new StubMapper(
                List.of(),
                List.of(new DeviceCountRow(1L, 10), new DeviceCountRow(1L, 11))
        ));

        assertThrows(IllegalStateException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> managerAdapter.findNames(Set.of(101L)));
        assertThrows(IllegalStateException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> deviceAdapter.countByLabIds(Set.of(1L)));
    }

    /**
     * avoidsDatabaseCallsFor空值标识符集合的处理逻辑。
     */
    @Test
    void avoidsDatabaseCallsForEmptyIdentifierSets() {
        AtomicInteger calls = new AtomicInteger();
        LabResourceMetadataMapper mapper = new LabResourceMetadataMapper() {
            /**
             * 查找Manager名称集合。
             * @param managerUserIds manager用户标识集合参数。
             * @return 当前操作得到的记录集合。
             */
            @Override
            public List<ManagerNameRow> findManagerNames(Set<Long> managerUserIds) {
                calls.incrementAndGet();
                return List.of();
            }

            /**
             * 统计设备By实验室标识集合。
             * @param labIds 实验室标识集合参数。
             * @return 当前操作得到的记录集合。
             */
            @Override
            public List<DeviceCountRow> countDevicesByLabIds(Set<Long> labIds) {
                calls.incrementAndGet();
                return List.of();
            }
        };
        var adapter = new LabMetadataService(mapper);

        assertEquals(0, adapter.findNames(Set.of()).size());
        assertEquals(0, adapter.countByLabIds(Set.of()).size());
        assertEquals(0, calls.get());
    }

    private record StubMapper(
            List<ManagerNameRow> managerRows,
            List<DeviceCountRow> deviceRows
    ) implements LabResourceMetadataMapper {
        /**
         * 查找Manager名称集合。
         * @param managerUserIds manager用户标识集合参数。
         * @return 当前操作得到的记录集合。
         */
        @Override
        public List<ManagerNameRow> findManagerNames(Set<Long> managerUserIds) {
            return managerRows;
        }

        /**
         * 统计设备By实验室标识集合。
         * @param labIds 实验室标识集合参数。
         * @return 当前操作得到的记录集合。
         */
        @Override
        public List<DeviceCountRow> countDevicesByLabIds(Set<Long> labIds) {
            return deviceRows;
        }
    }
}
