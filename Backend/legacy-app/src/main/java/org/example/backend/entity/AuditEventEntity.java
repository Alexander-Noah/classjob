package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditEventEntity {
    private Long id;
    private String aggregateType;
    private String aggregateId;
    private String action;
    private String resultStatus;
    private Long actorUserId;
    private String actorName;
    private String detailJson;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
}
