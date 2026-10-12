package org.example.backend.service;

public record ReviewReservationCommand(Long reservationId, String reviewRemark) {
}
