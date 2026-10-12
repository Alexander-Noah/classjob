package org.example.backend.teachingcoordination.api;

import org.example.backend.kernel.capability.CapabilityResult;

public interface EnvironmentSnapshotPort {
    CapabilityResult<EnvironmentSnapshot> findByTeachingTaskId(Long teachingTaskId);
}
