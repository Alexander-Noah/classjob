package org.example.backend.labresource.api;

import org.example.backend.kernel.capability.CapabilityResult;

import java.time.Instant;

public interface LabAvailabilityQuery {
    CapabilityResult<LabAvailabilityView> check(Long labId, int expectedCapacity, Instant startsAt, Instant endsAt);
}
