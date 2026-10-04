package org.example.backend.reservation.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.example.backend.reservation.application.CreateReservationCommand;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservationCreateRequest(
        @JsonAlias("applicant") String applicantName,
        String department,
        String contact,
        Long labId,
        @JsonAlias("date") LocalDate reservationDate,
        String timeRange,
        String scene,
        @JsonAlias("attendees") Integer attendeeCount,
        String reason
) {
    CreateReservationCommand toCommand() {
        return new CreateReservationCommand(
                applicantName, department, contact, labId, reservationDate, timeRange, scene, attendeeCount, reason
        );
    }
}
