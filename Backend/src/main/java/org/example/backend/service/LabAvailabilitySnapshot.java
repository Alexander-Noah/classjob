package org.example.backend.service;

public record LabAvailabilitySnapshot(Long labId, int capacity, boolean available) {
}
