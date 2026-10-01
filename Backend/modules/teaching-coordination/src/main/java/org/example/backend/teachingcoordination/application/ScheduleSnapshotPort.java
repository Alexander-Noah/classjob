package org.example.backend.teachingcoordination.application;

import org.example.backend.kernel.capability.CapabilityResult;

public interface ScheduleSnapshotPort {
    CapabilityResult<ScheduleSnapshot> findByTeachingTaskId(Long teachingTaskId);
}
