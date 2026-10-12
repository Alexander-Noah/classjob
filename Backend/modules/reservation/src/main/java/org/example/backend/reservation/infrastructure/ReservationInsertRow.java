package org.example.backend.reservation.infrastructure;

import org.example.backend.reservation.application.NewReservationRecord;

import java.time.LocalDate;

final class ReservationInsertRow {
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

    ReservationInsertRow(NewReservationRecord record) {
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

    Long getId() { return id; }
    void setId(Long id) { this.id = id; }
    Long getApplicantUserId() { return applicantUserId; }
    String getApplicantName() { return applicantName; }
    String getDepartment() { return department; }
    String getContact() { return contact; }
    Long getLabId() { return labId; }
    LocalDate getReservationDate() { return reservationDate; }
    String getTimeRange() { return timeRange; }
    String getScene() { return scene; }
    Integer getAttendeeCount() { return attendeeCount; }
    String getReason() { return reason; }
    String getStatus() { return status; }
    boolean isConflictFlag() { return conflictFlag; }
    String getReviewerName() { return reviewerName; }
    String getReviewRemark() { return reviewRemark; }
}
