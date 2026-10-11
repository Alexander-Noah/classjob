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

    public ReservationLabService(LabCatalogQuery catalog, LabAvailabilityQuery availability) {
        this.catalog = catalog;
        this.availability = availability;
    }

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

    public Map<Long, LaboratorySummary> findLaboratorySummaries(Set<Long> laboratoryIds) {
        if (laboratoryIds == null) {
            throw new IllegalArgumentException("laboratory ids are required");
        }
        if (laboratoryIds.isEmpty()) {
            return Map.of();
        }
        if (laboratoryIds.stream().anyMatch(id -> id == null || id <= 0)) {
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

    private List<LabView> requireLabs(List<LabView> labs) {
        if (labs == null) {
            throw new IllegalStateException("lab catalog is unavailable");
        }
        return labs;
    }

    private LabView requireLab(LabView lab) {
        if (lab == null) {
            throw new IllegalStateException("lab catalog returned malformed laboratory data");
        }
        return lab;
    }
}
