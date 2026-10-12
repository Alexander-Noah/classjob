package org.example.backend.service;

public record LabAccessScope(Long managerUserId) {
    public static LabAccessScope all() {
        return new LabAccessScope(null);
    }

    public static LabAccessScope managedBy(long managerUserId) {
        return new LabAccessScope(managerUserId);
    }
}
