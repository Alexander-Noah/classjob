package org.example.backend.labresource.application;

import org.example.backend.labresource.api.LabAccessScope;
import org.example.backend.labresource.api.LabCatalogQuery;
import org.example.backend.labresource.api.LabView;
import org.example.backend.labresource.application.port.out.DeviceCountPort;
import org.example.backend.labresource.application.port.out.LabRecord;
import org.example.backend.labresource.application.port.out.LabRecordReadPort;
import org.example.backend.labresource.application.port.out.ManagerNamePort;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Transactional(readOnly = true)
public final class DefaultLabCatalogQuery implements LabCatalogQuery {
    private final LabRecordReadPort records;
    private final ManagerNamePort managerNames;
    private final DeviceCountPort deviceCounts;

    public DefaultLabCatalogQuery(
            LabRecordReadPort records,
            ManagerNamePort managerNames,
            DeviceCountPort deviceCounts
    ) {
        this.records = Objects.requireNonNull(records, "records");
        this.managerNames = Objects.requireNonNull(managerNames, "managerNames");
        this.deviceCounts = Objects.requireNonNull(deviceCounts, "deviceCounts");
    }

    @Override
    public List<LabView> list(LabAccessScope scope) {
        Objects.requireNonNull(scope, "scope");
        List<LabRecord> found = requireRecords(records.findAll(scope.managerUserId()));
        ensureUniqueLabIds(found);
        return enrich(found);
    }

    @Override
    public LabView findById(long id) {
        LabRecord found = records.findById(id);
        if (found == null) {
            return null;
        }
        return enrich(List.of(found)).get(0);
    }

    private List<LabView> enrich(List<LabRecord> found) {
        Set<Long> labIds = new HashSet<>();
        Set<Long> managerUserIds = new HashSet<>();
        for (LabRecord record : found) {
            requireValidRecord(record);
            labIds.add(record.id());
            if (record.managerUserId() != null) {
                managerUserIds.add(record.managerUserId());
            }
        }

        Map<Long, String> names = managerUserIds.isEmpty()
                ? Map.of()
                : Objects.requireNonNull(managerNames.findNames(Set.copyOf(managerUserIds)), "manager names");
        Map<Long, Integer> counts = labIds.isEmpty()
                ? Map.of()
                : Objects.requireNonNull(deviceCounts.countByLabIds(Set.copyOf(labIds)), "device counts");
        validateManagerNames(managerUserIds, names);
        validateDeviceCounts(labIds, counts);

        return found.stream()
                .map(record -> toView(record, names, counts))
                .toList();
    }

    private List<LabRecord> requireRecords(List<LabRecord> found) {
        return Objects.requireNonNull(found, "lab records");
    }

    private void ensureUniqueLabIds(List<LabRecord> found) {
        Set<Long> identifiers = new HashSet<>();
        for (LabRecord record : found) {
            requireValidRecord(record);
            if (!identifiers.add(record.id())) {
                throw new IllegalStateException("duplicate lab id: " + record.id());
            }
        }
    }

    private void requireValidRecord(LabRecord record) {
        if (record == null || record.id() == null || record.id() <= 0) {
            throw new IllegalStateException("lab record must have a positive id");
        }
    }

    private void validateManagerNames(Set<Long> requested, Map<Long, String> names) {
        for (Map.Entry<Long, String> entry : names.entrySet()) {
            if (!requested.contains(entry.getKey()) || entry.getValue() == null) {
                throw new IllegalStateException("inconsistent manager metadata key: " + entry.getKey());
            }
        }
    }

    private void validateDeviceCounts(Set<Long> requested, Map<Long, Integer> counts) {
        for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
            if (!requested.contains(entry.getKey()) || entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalStateException("inconsistent device count key: " + entry.getKey());
            }
        }
    }

    private LabView toView(LabRecord record, Map<Long, String> names, Map<Long, Integer> counts) {
        return new LabView(
                record.id(),
                record.labCode(),
                record.labName(),
                record.building(),
                record.floor(),
                record.roomNo(),
                record.labType(),
                record.capacity(),
                record.managerUserId(),
                record.managerUserId() == null ? null : names.get(record.managerUserId()),
                record.openStatus(),
                counts.getOrDefault(record.id(), 0),
                record.remark(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
