package org.example.backend.teachingcoordination.application;

public record ReservationSnapshot(Long teachingTaskId, Long reservationId, String status) {
}
