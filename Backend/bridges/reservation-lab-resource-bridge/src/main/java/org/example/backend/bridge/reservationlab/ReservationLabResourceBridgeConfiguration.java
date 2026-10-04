package org.example.backend.bridge.reservationlab;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.labresource.api.LabAccessScope;
import org.example.backend.labresource.api.LabAvailabilityQuery;
import org.example.backend.labresource.api.LabCatalogQuery;
import org.example.backend.labresource.api.LabView;
import org.example.backend.reservation.application.LabAvailabilityPort;
import org.example.backend.reservation.application.LabAvailabilitySnapshot;
import org.example.backend.reservation.application.port.out.LaboratorySummary;
import org.example.backend.reservation.application.port.out.LaboratorySummaryPort;
import org.example.backend.reservation.application.port.out.ManagedLaboratoryIdsPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class ReservationLabResourceBridgeConfiguration {
    @Bean
    public LabAvailabilityPort bridgedLabAvailabilityPort(LabAvailabilityQuery query) {
        return (labId, expectedCapacity, timeRange) -> {
            var result = query.check(labId, expectedCapacity, timeRange.startsAt(), timeRange.endsAt());
            if (result.status() == CapabilityStatus.UNAVAILABLE) {
                return CapabilityResult.unavailable(result.message());
            }
            var view = result.value();
            return CapabilityResult.available(new LabAvailabilitySnapshot(view.labId(), view.capacity(), view.available()));
        };
    }

    @Bean
    @ConditionalOnBean(LabCatalogQuery.class)
    public ManagedLaboratoryIdsPort bridgedManagedLaboratoryIdsPort(LabCatalogQuery query) {
        return managerUserId -> {
            List<LabView> labs = requireLabs(query.list(LabAccessScope.managedBy(managerUserId)));
            Set<Long> laboratoryIds = new LinkedHashSet<>();
            for (LabView lab : labs) {
                Long labId = requireLab(lab).id();
                if (labId != null && labId > 0 && !laboratoryIds.add(labId)) {
                    throw new IllegalStateException("lab catalog returned a duplicate laboratory id");
                }
            }
            return Set.copyOf(laboratoryIds);
        };
    }

    @Bean
    @ConditionalOnBean(LabCatalogQuery.class)
    public LaboratorySummaryPort bridgedLaboratorySummaryPort(LabCatalogQuery query) {
        return laboratoryIds -> {
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
            for (LabView lab : requireLabs(query.list(LabAccessScope.all()))) {
                LabView resolvedLab = requireLab(lab);
                Long labId = resolvedLab.id();
                if (labId == null || !laboratoryIds.contains(labId)) {
                    throw new IllegalStateException("lab catalog returned an id outside the requested set");
                }
                if (summaries.putIfAbsent(labId, new LaboratorySummary(resolvedLab.labName(), resolvedLab.labCode())) != null) {
                    throw new IllegalStateException("lab catalog returned a duplicate laboratory id");
                }
            }
            return Map.copyOf(summaries);
        };
    }

    private static List<LabView> requireLabs(List<LabView> labs) {
        if (labs == null) {
            throw new IllegalStateException("lab catalog is unavailable");
        }
        return labs;
    }

    private static LabView requireLab(LabView lab) {
        if (lab == null) {
            throw new IllegalStateException("lab catalog returned malformed laboratory data");
        }
        return lab;
    }
}
