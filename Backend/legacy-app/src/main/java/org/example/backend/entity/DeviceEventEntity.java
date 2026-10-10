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
