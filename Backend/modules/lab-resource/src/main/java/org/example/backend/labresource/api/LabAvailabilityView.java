package org.example.backend.labresource.api;

public record LabAvailabilityView(Long labId, int capacity, boolean available) {
}
