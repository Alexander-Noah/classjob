/*
 * 实验室元数据实现模块。
 * 主要协作组件：LabResourceMetadataMapper。
 */
package org.example.backend.service;

import org.example.backend.service.DeviceCountPort;
import org.example.backend.service.ManagerNamePort;
import org.example.backend.mapper.LabResourceMetadataMapper;
import org.example.backend.entity.DeviceCountRow;
import org.example.backend.entity.ManagerNameRow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class LabMetadataService implements ManagerNamePort, DeviceCountPort {
    private final LabResourceMetadataMapper mapper;

    public LabMetadataService(LabResourceMetadataMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public Map<Long, String> findNames(Set<Long> managerUserIds) {
        if (managerUserIds.isEmpty()) {
            return Map.of();
        }
        List<ManagerNameRow> rows = Objects.requireNonNull(
                mapper.findManagerNames(Set.copyOf(managerUserIds)),
                "manager name rows"
        );
        Map<Long, String> names = new LinkedHashMap<>();
        for (ManagerNameRow row : rows) {
            if (row == null || row.managerUserId() == null || row.managerName() == null) {
                throw new IllegalStateException("Manager metadata contains null values");
            }
            if (names.putIfAbsent(row.managerUserId(), row.managerName()) != null) {
                throw new IllegalStateException("Duplicate manager metadata for id " + row.managerUserId());
            }
        }
        return Map.copyOf(names);
    }

    @Override
    public Map<Long, Integer> countByLabIds(Set<Long> labIds) {
        if (labIds.isEmpty()) {
            return Map.of();
        }
        List<DeviceCountRow> rows = Objects.requireNonNull(
                mapper.countDevicesByLabIds(Set.copyOf(labIds)),
                "device count rows"
        );
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (DeviceCountRow row : rows) {
            if (row == null || row.labId() == null || row.deviceCount() == null || row.deviceCount() < 0) {
                throw new IllegalStateException("Device count metadata contains invalid values");
            }
            if (counts.putIfAbsent(row.labId(), row.deviceCount()) != null) {
                throw new IllegalStateException("Duplicate device count metadata for lab id " + row.labId());
            }
        }
        return Map.copyOf(counts);
    }
}
