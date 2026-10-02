package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeEntity {
    private Long id;
    private String title;
    private String type;
    private String noticeType;
    private String target;
    private String targetRole;
    private String priority;
    private String sourceModule;
    private String sourceId;
    private String businessType;
    private String content;
    private String status;
    private String publishStatus;
    private LocalDateTime publishTime;
    private LocalDateTime withdrawnAt;
    private LocalDateTime archivedAt;
    private Boolean deleted;
    private LocalDateTime deletedAt;
    private Long createdBy;
    private String tagType;
    private Integer recipientCount;
    private Integer readCount;
    private Integer unreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
