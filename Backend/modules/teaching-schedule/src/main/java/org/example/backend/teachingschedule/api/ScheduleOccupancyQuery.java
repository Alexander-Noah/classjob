package org.example.backend.teachingschedule.api;

import org.example.backend.kernel.capability.CapabilityResult;

public interface ScheduleOccupancyQuery {
    CapabilityResult<ScheduleOccupancyView> check(ScheduleOccupancyRequest request);
}
