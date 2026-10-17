/*
 * 审批会签数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、businessType、businessId、businessTitle、businessStatus、assignerId；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApprovalCountersignEntity {
    private Long id;
    private String businessType;
    private String businessId;
    private String businessTitle;
    private String businessStatus;
    private String assignerId;
    private String assignerName;
    private String assigneeId;
    private String assigneeName;
    private String reason;
    private String status;
    private String result;
    private String resultRemark;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getTagType() {
        if ("已同意".equals(status) || "已同意".equals(result)) return "success";
        if ("已退回".equals(status) || "已退回".equals(result)) return "danger";
        if ("已取消".equals(status)) return "info";
        return "warning";
    }
}
