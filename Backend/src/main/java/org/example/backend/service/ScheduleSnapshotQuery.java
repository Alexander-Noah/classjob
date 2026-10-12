package org.example.backend.service;

import org.example.backend.service.CapabilityResult;

public interface ScheduleSnapshotQuery {
    CapabilityResult<ScheduleSnapshotView> findByTeachingTaskId(Long teachingTaskId);
}
