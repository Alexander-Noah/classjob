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

        assertThrows(IllegalStateException.class, () -> managerAdapter.findNames(Set.of(101L)));
        assertThrows(IllegalStateException.class, () -> deviceAdapter.countByLabIds(Set.of(1L)));
    }

    @Test
    void avoidsDatabaseCallsForEmptyIdentifierSets() {
        AtomicInteger calls = new AtomicInteger();
        LabResourceMetadataMapper mapper = new LabResourceMetadataMapper() {
            @Override
            public List<ManagerNameRow> findManagerNames(Set<Long> managerUserIds) {
                calls.incrementAndGet();
                return List.of();
            }

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
        @Override
        public List<ManagerNameRow> findManagerNames(Set<Long> managerUserIds) {
            return managerRows;
        }

        @Override
        public List<DeviceCountRow> countDevicesByLabIds(Set<Long> labIds) {
            return deviceRows;
        }
    }
}
