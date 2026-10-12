package org.example.backend.service;

import org.example.backend.service.CapabilityResult;

public interface EnvironmentSnapshotPort {
    CapabilityResult<EnvironmentSnapshot> findByTeachingTaskId(Long teachingTaskId);
}
