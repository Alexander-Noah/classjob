package org.example.backend.teachingschedule.api;

import org.example.backend.kernel.capability.CapabilityResult;

public interface ScheduleSnapshotQuery {
    CapabilityResult<ScheduleSnapshotView> findByTeachingTaskId(Long teachingTaskId);
}
