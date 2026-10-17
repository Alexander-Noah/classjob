/*
 * 通知公告数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、noticeId、userId、username、realName、roleCode；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeRecipientEntity {
    private Long id;
    private Long noticeId;
    private Integer userId;
    private String username;
    private String realName;
    private String roleCode;
    private String readStatus;
    private LocalDateTime readTime;
    private Boolean archived;
    private LocalDateTime archivedAt;
    private Boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
    private String tagType;
}
