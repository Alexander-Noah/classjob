package org.example.backend.service;

import org.example.backend.service.CapabilityResult;

public interface ScheduleOccupancyQuery {
    CapabilityResult<ScheduleOccupancyView> check(ScheduleOccupancyRequest request);
}
