package org.example.backend.service;

import org.example.backend.service.CapabilityResult;

public interface ScheduleSnapshotPort {
    CapabilityResult<ScheduleSnapshot> findByTeachingTaskId(Long teachingTaskId);
}
