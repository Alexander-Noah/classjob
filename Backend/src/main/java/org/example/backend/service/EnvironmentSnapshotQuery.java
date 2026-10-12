package org.example.backend.service;

import org.example.backend.service.CapabilityResult;

public interface EnvironmentSnapshotQuery {
    CapabilityResult<EnvironmentSnapshotView> findByTeachingTaskId(Long teachingTaskId);
}
