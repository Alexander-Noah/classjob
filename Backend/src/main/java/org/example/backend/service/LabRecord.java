package org.example.backend.service;

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
