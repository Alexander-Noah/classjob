/*
 * 实验室预约实现模块。
 */
package org.example.backend.mapper;

import org.example.backend.service.NewReservationRecord;

import java.time.LocalDate;

public final class ReservationInsertRow {
    private Long id;
    private final Long applicantUserId;
    private final String applicantName;
    private final String department;
    private final String contact;
    private final Long labId;
    private final LocalDate reservationDate;
    private final String timeRange;
    private final String scene;
    private final Integer attendeeCount;
    private final String reason;
    private final String status;
    private final boolean conflictFlag;
    private final String reviewerName;
    private final String reviewRemark;

    /**
     * 构造 ReservationInsertRow，初始化当前实例。
     * @param record 记录参数。
     */
    public ReservationInsertRow(NewReservationRecord record) {
        applicantUserId = record.applicantUserId();
        applicantName = record.applicantName();
        department = record.department();
        contact = record.contact();
        labId = record.labId();
        reservationDate = record.reservationDate();
        timeRange = record.timeRange();
        scene = record.scene();
        attendeeCount = record.attendeeCount();
        reason = record.reason();
        status = record.status().storageValue();
        conflictFlag = record.conflictFlag();
        reviewerName = record.reviewerName();
        reviewRemark = record.reviewRemark();
    }

    /**
     * 获取标识。
     * @return Long 类型的处理结果。
     */
    public Long getId() { return id; }
    /**
     * 设置标识。
     * @param id 标识参数。
     */
    public void setId(Long id) { this.id = id; }
    /**
     * 获取Applicant用户标识。
     * @return Long 类型的处理结果。
     */
    public Long getApplicantUserId() { return applicantUserId; }
    /**
     * 获取Applicant名称。
     * @return String 类型的处理结果。
     */
    public String getApplicantName() { return applicantName; }
    /**
     * 获取部门。
     * @return String 类型的处理结果。
     */
    public String getDepartment() { return department; }
    /**
     * 获取Contact。
     * @return String 类型的处理结果。
     */
    public String getContact() { return contact; }
    /**
     * 获取实验室标识。
     * @return Long 类型的处理结果。
     */
    public Long getLabId() { return labId; }
    /**
     * 获取预约日期。
     * @return LocalDate 类型的处理结果。
     */
    public LocalDate getReservationDate() { return reservationDate; }
    /**
     * 获取时间范围。
     * @return String 类型的处理结果。
     */
    public String getTimeRange() { return timeRange; }
    /**
     * 获取Scene。
     * @return String 类型的处理结果。
     */
    public String getScene() { return scene; }
    /**
     * 获取Attendee数量。
     * @return Integer 类型的处理结果。
     */
    public Integer getAttendeeCount() { return attendeeCount; }
    /**
     * 获取原因。
     * @return String 类型的处理结果。
     */
    public String getReason() { return reason; }
    /**
     * 获取状态。
     * @return String 类型的处理结果。
     */
    public String getStatus() { return status; }
    /**
     * 判断是否为Conflict标记。
     * @return 当前判断的布尔结果。
     */
    public boolean isConflictFlag() { return conflictFlag; }
    /**
     * 获取Reviewer名称。
     * @return String 类型的处理结果。
     */
    public String getReviewerName() { return reviewerName; }
    /**
     * 获取ReviewRemark。
     * @return String 类型的处理结果。
     */
    public String getReviewRemark() { return reviewRemark; }
}
