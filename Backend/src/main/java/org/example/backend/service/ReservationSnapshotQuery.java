package org.example.backend.service;

import org.example.backend.service.CapabilityResult;

public interface ReservationSnapshotQuery {
    CapabilityResult<ReservationSnapshotView> findByTeachingTaskId(Long teachingTaskId);
}
