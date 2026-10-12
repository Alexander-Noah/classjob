package org.example.backend.service;

import org.example.backend.service.CapabilityResult;

public interface ReservationSnapshotPort {
    CapabilityResult<ReservationSnapshot> findByTeachingTaskId(Long teachingTaskId);
}
