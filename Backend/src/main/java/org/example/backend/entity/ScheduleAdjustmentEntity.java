/*
 * 调课申请数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、course、originalTime、targetTime、reason、status；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleAdjustmentEntity {
    private Long id;
    private String course;
    private String originalTime;
    private String targetTime;
    private String reason;
    private String status;
    private String tagType;
    private Integer flowStep;
    private String reviewer;
    private String teacherName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
