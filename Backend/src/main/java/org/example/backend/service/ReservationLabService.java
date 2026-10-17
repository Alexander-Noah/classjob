/*
 * 实验室预约实现模块。
 * 主要协作组件：LabCatalogQuery、LabAvailabilityQuery。
 */
package org.example.backend.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReservationLabService {
    private final LabCatalogQuery catalog;
    private final LabAvailabilityQuery availability;

    /**
     * 构造 ReservationLabService，初始化当前实例。
     * @param catalog catalog参数。
     * @param availability availability参数。
     */
    public ReservationLabService(LabCatalogQuery catalog, LabAvailabilityQuery availability) {
        this.catalog = catalog;
        this.availability = availability;
    }

    /**
     * 检查Availability。
     * @param labId 实验室标识参数。
     * @param expectedCapacity 预期Capacity参数。
     * @param timeRange 时间区间参数。
     * @return CapabilityResult<LabAvailabilitySnapshot> 类型的处理结果。
     */
    public CapabilityResult<LabAvailabilitySnapshot> checkAvailability(
            long labId,
            int expectedCapacity,
            TimeRange timeRange
    ) {
        var result = availability.check(labId, expectedCapacity, timeRange.startsAt(), timeRange.endsAt());
        if (result.status() == CapabilityStatus.UNAVAILABLE) {
            return CapabilityResult.unavailable(result.message());
        }
        var view = result.value();
        return CapabilityResult.available(new LabAvailabilitySnapshot(view.labId(), view.capacity(), view.available()));
    }

    /**
     * 查找ManagedLaboratory标识集合。
     * @param managerUserId manager用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    public Set<Long> findManagedLaboratoryIds(long managerUserId) {
        List<LabView> labs = requireLabs(catalog.list(LabAccessScope.managedBy(managerUserId)));
        Set<Long> ids = new LinkedHashSet<>();
        for (LabView lab : labs) {
            Long id = requireLab(lab).id();
            if (id != null && id > 0 && !ids.add(id)) {
                throw new IllegalStateException("lab catalog returned a duplicate laboratory id");
            }
        }
        return Set.copyOf(ids);
    }

    /**
     * 查找Laboratory摘要。
     * @param laboratoryIds laboratory标识集合参数。
     * @return 当前操作返回的键值结果。
     */
    public Map<Long, LaboratorySummary> findLaboratorySummaries(Set<Long> laboratoryIds) {
        if (laboratoryIds == null) {
            throw new IllegalArgumentException("laboratory ids are required");
        }
        if (laboratoryIds.isEmpty()) {
            return Map.of();
        }
        if (laboratoryIds.stream().anyMatch(/** 处理 anyMatch 调用中的回调逻辑。 入参：id。 */ id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("laboratory ids must be positive");
        }
        Map<Long, LaboratorySummary> summaries = new LinkedHashMap<>();
        for (LabView lab : requireLabs(catalog.list(LabAccessScope.all()))) {
            LabView resolved = requireLab(lab);
            Long id = resolved.id();
            if (id == null || !laboratoryIds.contains(id)) {
                continue;
            }
            if (summaries.putIfAbsent(id, new LaboratorySummary(resolved.labName(), resolved.labCode())) != null) {
                throw new IllegalStateException("lab catalog returned a duplicate laboratory id");
            }
        }
        return Map.copyOf(summaries);
    }

    /**
     * 要求提供实验室。
     * @param labs 实验室参数。
     * @return 当前操作得到的记录集合。
     */
    private List<LabView> requireLabs(List<LabView> labs) {
        if (labs == null) {
            throw new IllegalStateException("lab catalog is unavailable");
        }
        return labs;
    }

    /**
     * 要求提供实验室。
     * @param lab 实验室参数。
     * @return LabView 类型的处理结果。
     */
    private LabView requireLab(LabView lab) {
        if (lab == null) {
            throw new IllegalStateException("lab catalog returned malformed laboratory data");
        }
        return lab;
    }
}
