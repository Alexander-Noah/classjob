package org.example.backend.teachingcoordination.api;

import org.example.backend.kernel.capability.CapabilityResult;

public interface ReservationSnapshotPort {
    CapabilityResult<ReservationSnapshot> findByTeachingTaskId(Long teachingTaskId);
}
