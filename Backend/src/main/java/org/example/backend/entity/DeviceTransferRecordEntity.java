/*
 * 设备调拨数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、deviceId、deviceCode、deviceName、transferType、fromLabId；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceTransferRecordEntity {
    private Long id;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private String transferType;
    private Long fromLabId;
    private String fromLabName;
    private Long toLabId;
    private String toLabName;
    private Long fromOwnerUserId;
    private String fromOwnerName;
    private Long toOwnerUserId;
    private String toOwnerName;
    private String fromLocation;
    private String toLocation;
    private String reason;
    private String operatorName;
    private String transferAt;
    private LocalDateTime createdAt;
}
