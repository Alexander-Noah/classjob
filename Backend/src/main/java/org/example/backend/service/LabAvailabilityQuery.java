package org.example.backend.service;

import org.example.backend.service.CapabilityResult;

import java.time.Instant;

public interface LabAvailabilityQuery {
    CapabilityResult<LabAvailabilityView> check(Long labId, int expectedCapacity, Instant startsAt, Instant endsAt);
}
