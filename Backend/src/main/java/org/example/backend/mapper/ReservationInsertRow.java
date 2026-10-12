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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicantUserId() { return applicantUserId; }
    public String getApplicantName() { return applicantName; }
    public String getDepartment() { return department; }
    public String getContact() { return contact; }
    public Long getLabId() { return labId; }
    public LocalDate getReservationDate() { return reservationDate; }
    public String getTimeRange() { return timeRange; }
    public String getScene() { return scene; }
    public Integer getAttendeeCount() { return attendeeCount; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public boolean isConflictFlag() { return conflictFlag; }
    public String getReviewerName() { return reviewerName; }
    public String getReviewRemark() { return reviewRemark; }
}
