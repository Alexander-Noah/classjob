package org.example.backend.environmentreadiness.api;

import org.example.backend.kernel.capability.CapabilityResult;

public interface EnvironmentSnapshotQuery {
    CapabilityResult<EnvironmentSnapshotView> findByTeachingTaskId(Long teachingTaskId);
}
