package org.example.backend.teachingcoordination.api;

public record ReservationSnapshot(Long teachingTaskId, Long reservationId, String status) {
}
