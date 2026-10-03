package org.example.backend.reservation.application;

public record ReviewReservationCommand(Long reservationId, String reviewRemark) {
}
