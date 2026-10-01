package org.example.backend.teachingcoordination.application;

import org.example.backend.kernel.capability.CapabilityResult;

public interface EnvironmentSnapshotPort {
    CapabilityResult<EnvironmentSnapshot> findByTeachingTaskId(Long teachingTaskId);
}
