package org.example.backend.reservation.application.port.out;

import java.util.Set;

public interface ManagedLaboratoryIdsPort {
    Set<Long> findByManagerUserId(long managerUserId);
}
