/*
 * 设备事件数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、deviceId、eventType、occurredAt、operatorUserId、operatorName；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceEventEntity {
    private Long id;
    private Long deviceId;
    private String eventType;
    private LocalDateTime occurredAt;
    private Long operatorUserId;
    private String operatorName;
    private String resultStatus;
    private String beforeState;
    private String afterState;
    private String remark;
    private LocalDateTime createdAt;
}
