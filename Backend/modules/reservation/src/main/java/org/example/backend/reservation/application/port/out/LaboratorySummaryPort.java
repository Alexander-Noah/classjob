package org.example.backend.reservation.application.port.out;

import java.util.Map;
import java.util.Set;

public interface LaboratorySummaryPort {
    Map<Long, LaboratorySummary> findByIds(Set<Long> laboratoryIds);
}
