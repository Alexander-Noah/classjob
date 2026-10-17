/*
 * 实验室预约数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.example.backend.service.CreateReservationCommand;

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
