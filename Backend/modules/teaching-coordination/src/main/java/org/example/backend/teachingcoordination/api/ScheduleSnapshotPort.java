package org.example.backend.teachingcoordination.api;

import org.example.backend.kernel.capability.CapabilityResult;

public interface ScheduleSnapshotPort {
    CapabilityResult<ScheduleSnapshot> findByTeachingTaskId(Long teachingTaskId);
}
