/*
 * 实验室数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
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
