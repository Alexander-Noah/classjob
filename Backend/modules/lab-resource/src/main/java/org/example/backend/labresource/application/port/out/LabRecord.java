package org.example.backend.labresource.application.port.out;

import java.time.LocalDateTime;

public record LabRecord(
        Long id,
        String labCode,
        String labName,
        String building,
        String floor,
        String roomNo,
        String labType,
        Integer capacity,
        Long managerUserId,
        String openStatus,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
