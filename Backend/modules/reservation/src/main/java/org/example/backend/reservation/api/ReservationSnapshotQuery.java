package org.example.backend.reservation.api;

import org.example.backend.kernel.capability.CapabilityResult;

public interface ReservationSnapshotQuery {
    CapabilityResult<ReservationSnapshotView> findByTeachingTaskId(Long teachingTaskId);
}
