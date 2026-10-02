package org.example.backend.kernel.capability;

public record CapabilityResult<T>(
        CapabilityStatus status,
        T value,
        String errorCode,
        String message
) {

    public static <T> CapabilityResult<T> available(T value) {
        return new CapabilityResult<>(CapabilityStatus.AVAILABLE, value, null, null);
    }

    public static <T> CapabilityResult<T> unavailable(String message) {
        return new CapabilityResult<>(
                CapabilityStatus.UNAVAILABLE,
                null,
                "CAPABILITY_UNAVAILABLE",
                message
        );
    }
}
