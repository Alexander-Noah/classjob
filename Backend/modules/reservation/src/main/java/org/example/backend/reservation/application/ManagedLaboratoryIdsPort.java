package org.example.backend.reservation.application;

import java.util.Set;

public interface ManagedLaboratoryIdsPort {
    Set<Long> findByManagerUserId(long managerUserId);
}
