package org.example.backend.reservation.adapter.in.web;

import org.example.backend.reservation.api.ReservationView;
import org.example.backend.reservation.domain.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ReservationResponse(
        Long id,
        Long applicantUserId,
        String applicantName,
        String applicant,
        String department,
        String contact,
        Long labId,
        LocalDate reservationDate,
        String date,
        String timeRange,
        String scene,
        Integer attendeeCount,
        Integer attendees,
        String reason,
        String status,
        int conflictFlag,
        boolean conflict,
        String reviewerName,
        String reviewer,
        String reviewRemark,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String submittedAt,
        String reviewedAt,
        String labName,
        String labCode,
        String lab,
        String tagType
) {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    static ReservationResponse from(ReservationView view) {
        if (view == null) {
            return null;
        }
        String reviewerName = reviewerName(view.reviewerName());
        String status = view.status() == null ? null : view.status().storageValue();
        return new ReservationResponse(
                view.id(), view.applicantUserId(), view.applicantName(), view.applicantName(), view.department(), view.contact(),
                view.labId(), view.reservationDate(), date(view.reservationDate()), view.timeRange(), view.scene(),
                view.attendeeCount(), view.attendeeCount(), view.reason(), status, view.conflictFlag() ? 1 : 0,
                view.conflictFlag(), reviewerName, reviewerName, view.reviewRemark(), view.reviewRemark(),
                view.createdAt(), view.updatedAt(), submittedAt(view.createdAt()), reviewedAt(view.updatedAt(), reviewerName),
                view.labName(), view.labCode(), lab(view.labId(), view.labName(), view.labCode()), tagType(view.status())
        );
    }

    private static String date(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private static String reviewerName(String value) {
        return value == null || value.isBlank() ? "待审核" : value;
    }

    private static String submittedAt(LocalDateTime value) {
        return value == null ? "" : value.format(TIME_FORMATTER);
    }

    private static String reviewedAt(LocalDateTime value, String reviewerName) {
        return value == null || "待审核".equals(reviewerName) ? "-" : value.format(TIME_FORMATTER);
    }

    private static String lab(Long labId, String labName, String labCode) {
        if (labName != null && !labName.isBlank()) {
            if (labCode == null || labCode.isBlank() || labName.startsWith(labCode)) {
                return labName;
            }
            return labCode + " " + labName;
        }
        return labId == null ? "" : String.valueOf(labId);
    }

    private static String tagType(ReservationStatus status) {
        if (status == ReservationStatus.APPROVED || status == ReservationStatus.COMPLETED) {
            return "success";
        }
        if (status == ReservationStatus.CONFLICT) {
            return "danger";
        }
        if (status == ReservationStatus.REJECTED || status == ReservationStatus.CANCELLED || status == ReservationStatus.EXPIRED) {
            return "info";
        }
        return "warning";
    }
}
