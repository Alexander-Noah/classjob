package org.example.backend.entity;

import java.util.Arrays;

public enum ReservationStatus {
    PENDING("待审核", false),
    CONFLICT("冲突", false),
    APPROVED("已通过", true),
    REJECTED("已驳回", true),
    EXPIRED("已过期", true),
    COUNTERSIGNING("加签中", false),
    COMPLETED("已完成", true),
    CANCELLED("已取消", true);

    private final String storageValue;
    private final boolean finalStatus;

    ReservationStatus(String storageValue, boolean finalStatus) {
        this.storageValue = storageValue;
        this.finalStatus = finalStatus;
    }

    public String storageValue() {
        return storageValue;
    }

    public boolean isFinal() {
        return finalStatus;
    }

    public static ReservationStatus fromStorageValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Reservation status is required");
        }
        return Arrays.stream(values())
                .filter(status -> status.storageValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported reservation status: " + value));
    }
}
