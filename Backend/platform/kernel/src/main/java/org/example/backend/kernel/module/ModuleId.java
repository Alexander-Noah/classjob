package org.example.backend.kernel.module;

public record ModuleId(String value) {

    public ModuleId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("module id must not be blank");
        }
        value = value.trim();
    }
}
