/*
 * 实验室预约数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、applicantUserId、applicantName、department、contact、labId；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class ReservationsEntity {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private Long id;
    private Long applicantUserId;
    @JsonAlias("applicant")
    private String applicantName;
    private String department;
    private String contact;
    private Long labId;
    @JsonAlias("date")
    private LocalDate reservationDate;
    private String timeRange;
    private String scene;
    @JsonAlias("attendees")
    private Integer attendeeCount;
    private String reason;
    private String status;
    private Integer conflictFlag;
    @JsonAlias("reviewer")
    private String reviewerName;
    @JsonAlias("note")
    private String reviewRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @JsonAlias("lab")
    private String labName;
    private String labCode;

    /**
     * 获取Applicant。
     * @return String 类型的处理结果。
     */
    public String getApplicant() {
        return applicantName;
    }

    /**
     * 获取实验室。
     * @return String 类型的处理结果。
     */
    public String getLab() {
        if (labName != null && !labName.isBlank()) {
            if (labCode == null || labCode.isBlank() || labName.startsWith(labCode)) {
                return labName;
            }
            return labCode + " " + labName;
        }
        return labId == null ? "" : String.valueOf(labId);
    }

    /**
     * 获取日期。
     * @return String 类型的处理结果。
     */
    public String getDate() {
        return reservationDate == null ? "" : reservationDate.format(DATE_FORMATTER);
    }

    /**
     * 获取Attendees。
     * @return Integer 类型的处理结果。
     */
    public Integer getAttendees() {
        return attendeeCount;
    }

    /**
     * 获取Tag类型。
     * @return String 类型的处理结果。
     */
    public String getTagType() {
        if ("已通过".equals(status)) return "success";
        if ("已完成".equals(status)) return "success";
        if ("冲突".equals(status)) return "danger";
        if ("已驳回".equals(status) || "已取消".equals(status) || "已过期".equals(status)) return "info";
        return "warning";
    }

    /**
     * 获取Conflict。
     * @return 当前判断的布尔结果。
     */
    public Boolean getConflict() {
        return conflictFlag != null && conflictFlag == 1;
    }

    /**
     * 设置Conflict。
     * @param conflict conflict参数。
     */
    public void setConflict(Boolean conflict) {
        this.conflictFlag = Boolean.TRUE.equals(conflict) ? 1 : 0;
    }

    /**
     * 获取Reviewer。
     * @return String 类型的处理结果。
     */
    public String getReviewer() {
        return reviewerName == null || reviewerName.isBlank() ? "待审核" : reviewerName;
    }

    /**
     * 获取SubmittedAt。
     * @return String 类型的处理结果。
     */
    public String getSubmittedAt() {
        return createdAt == null ? "" : createdAt.format(TIME_FORMATTER);
    }

    /**
     * 获取ReviewedAt。
     * @return String 类型的处理结果。
     */
    public String getReviewedAt() {
        if (updatedAt == null || reviewerName == null || "待审核".equals(reviewerName)) {
            return "-";
        }
        return updatedAt.format(TIME_FORMATTER);
    }

    /**
     * 获取Note。
     * @return String 类型的处理结果。
     */
    public String getNote() {
        return reviewRemark;
    }
}
