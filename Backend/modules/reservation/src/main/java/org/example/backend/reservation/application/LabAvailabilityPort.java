package org.example.backend.reservation.application;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.reservation.domain.TimeRange;

public interface LabAvailabilityPort {
    CapabilityResult<LabAvailabilitySnapshot> check(Long labId, int expectedCapacity, TimeRange timeRange);
}
