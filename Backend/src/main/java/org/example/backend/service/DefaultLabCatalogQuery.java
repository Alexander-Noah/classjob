/*
 * 实验室实现模块。
 * 主要协作组件：LabRecordReadService、ManagerNamePort、DeviceCountPort。
 */
package org.example.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DefaultLabCatalogQuery implements LabCatalogQuery {
    private final LabRecordReadService records;
    private final ManagerNamePort managerNames;
    private final DeviceCountPort deviceCounts;

    /**
     * 构造 DefaultLabCatalogQuery，初始化当前实例。
     * @param records 记录集合参数。
     * @param managerNames manager名称集合参数。
     * @param deviceCounts 设备数量参数。
     */
    public DefaultLabCatalogQuery(
            LabRecordReadService records,
            ManagerNamePort managerNames,
            DeviceCountPort deviceCounts
    ) {
        this.records = Objects.requireNonNull(records, "records");
        this.managerNames = Objects.requireNonNull(managerNames, "managerNames");
        this.deviceCounts = Objects.requireNonNull(deviceCounts, "deviceCounts");
    }

    /**
     * 列出当前操作。
     * @param scope scope参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    public List<LabView> list(LabAccessScope scope) {
        Objects.requireNonNull(scope, "scope");
        List<LabRecord> found = requireRecords(records.findAll(scope.managerUserId()));
        ensureUniqueLabIds(found);
        return enrich(found);
    }

    /**
     * 查找By标识。
     * @param id 标识参数。
     * @param scope scope参数。
     * @return LabView 类型的处理结果。
     */
    @Override
    public LabView findById(long id, LabAccessScope scope) {
        Objects.requireNonNull(scope, "scope");
        LabRecord found = records.findById(id, scope.managerUserId());
        if (found == null) {
            return null;
        }
        return enrich(List.of(found)).get(0);
    }

    /**
     * enrich的处理逻辑。
     * @param found found参数。
     * @return 当前操作得到的记录集合。
     */
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
                .map(/** 将当前条目转换为目标结果。 入参：record。 */ record -> toView(record, names, counts))
                .toList();
    }

    /**
     * 要求提供记录集合。
     * @param found found参数。
     * @return 当前操作得到的记录集合。
     */
    private List<LabRecord> requireRecords(List<LabRecord> found) {
        return Objects.requireNonNull(found, "lab records");
    }

    /**
     * 确保满足Unique实验室标识集合。
     * @param found found参数。
     */
    private void ensureUniqueLabIds(List<LabRecord> found) {
        Set<Long> identifiers = new HashSet<>();
        for (LabRecord record : found) {
            requireValidRecord(record);
            if (!identifiers.add(record.id())) {
                throw new IllegalStateException("duplicate lab id: " + record.id());
            }
        }
    }

    /**
     * 要求提供有效记录。
     * @param record 记录参数。
     */
    private void requireValidRecord(LabRecord record) {
        if (record == null || record.id() == null || record.id() <= 0) {
            throw new IllegalStateException("lab record must have a positive id");
        }
    }

    /**
     * 校验Manager名称集合。
     * @param requested requested参数。
     * @param names 名称集合参数。
     */
    private void validateManagerNames(Set<Long> requested, Map<Long, String> names) {
        for (Map.Entry<Long, String> entry : names.entrySet()) {
            if (!requested.contains(entry.getKey()) || entry.getValue() == null) {
                throw new IllegalStateException("inconsistent manager metadata key: " + entry.getKey());
            }
        }
    }

    /**
     * 校验设备数量。
     * @param requested requested参数。
     * @param counts 数量参数。
     */
    private void validateDeviceCounts(Set<Long> requested, Map<Long, Integer> counts) {
        for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
            if (!requested.contains(entry.getKey()) || entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalStateException("inconsistent device count key: " + entry.getKey());
            }
        }
    }

    /**
     * 转换为视图。
     * @param record 记录参数。
     * @param names 名称集合参数。
     * @param counts 数量参数。
     * @return LabView 类型的处理结果。
     */
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
