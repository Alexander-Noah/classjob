package org.example.backend.labresource.api;

import java.time.LocalDateTime;

public record LabView(
        Long id,
        String labCode,
        String labName,
        String building,
        String floor,
        String roomNo,
        String labType,
        Integer capacity,
        Long managerUserId,
        String managerName,
        String openStatus,
        int deviceCount,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
