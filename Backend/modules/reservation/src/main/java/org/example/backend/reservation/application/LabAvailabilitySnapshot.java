package org.example.backend.reservation.application;

public record LabAvailabilitySnapshot(Long labId, int capacity, boolean available) {
}
