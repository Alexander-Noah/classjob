package org.example.backend.labresource.application;

import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.labresource.application.LabRecord;
import org.example.backend.labresource.application.LabRecordReadPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultLabAvailabilityQueryTest {
    private static final Instant STARTS_AT = Instant.parse("2026-10-10T01:00:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-10-10T03:00:00Z");

    @Test
    void acceptsEligibleStatusesWhenCapacityIsSufficient() {
        for (String status : List.of("开放", "预约中", "使用中")) {
            var query = new DefaultLabAvailabilityQuery(records(lab(48, status)));

            var result = query.check(7L, 48, STARTS_AT, ENDS_AT);

            assertEquals(CapabilityStatus.AVAILABLE, result.status());
            assertEquals(48, result.value().capacity());
            assertTrue(result.value().available(), status);
        }
    }

    @Test
    void rejectsInsufficientCapacityAndNonEligibleStatuses() {
        var insufficient = new DefaultLabAvailabilityQuery(records(lab(47, "开放")))
                .check(7L, 48, STARTS_AT, ENDS_AT);
        assertFalse(insufficient.value().available());

        for (String status : List.of("维护中", "停用", "未知状态", " ")) {
            var result = new DefaultLabAvailabilityQuery(records(lab(100, status)))
                    .check(7L, 48, STARTS_AT, ENDS_AT);
            assertFalse(result.value().available(), status);
        }
    }

    @Test
    void distinguishesMissingLabFromMissingModule() {
        var result = new DefaultLabAvailabilityQuery(records(null))
                .check(999L, 1, STARTS_AT, ENDS_AT);

        assertEquals(CapabilityStatus.AVAILABLE, result.status());
        assertEquals(999L, result.value().labId());
        assertEquals(0, result.value().capacity());
        assertFalse(result.value().available());
    }

    @Test
    void validatesArgumentsBeforeReadingTheDatabase() {
        AtomicInteger reads = new AtomicInteger();
        LabRecordReadPort port = new LabRecordReadPort() {
            @Override
            public List<LabRecord> findAll(Long managerUserId) {
                return List.of();
            }

            @Override
            public LabRecord findById(long id, Long managerUserId) {
                reads.incrementAndGet();
                return lab(48, "开放");
            }
        };
        var query = new DefaultLabAvailabilityQuery(port);

        assertThrows(IllegalArgumentException.class, () -> query.check(null, 1, STARTS_AT, ENDS_AT));
        assertThrows(IllegalArgumentException.class, () -> query.check(7L, 0, STARTS_AT, ENDS_AT));
        assertThrows(IllegalArgumentException.class, () -> query.check(7L, 1, null, ENDS_AT));
        assertThrows(IllegalArgumentException.class, () -> query.check(7L, 1, ENDS_AT, STARTS_AT));
        assertEquals(0, reads.get());
    }

    private LabRecordReadPort records(LabRecord record) {
        return new LabRecordReadPort() {
            @Override
            public List<LabRecord> findAll(Long managerUserId) {
                return record == null ? List.of() : List.of(record);
            }

            @Override
            public LabRecord findById(long id, Long managerUserId) {
                return record;
            }
        };
    }

    private LabRecord lab(Integer capacity, String status) {
        return new LabRecord(
                7L,
                "LAB-007",
                "网络实验室",
                "实训楼",
                "3",
                "305",
                "网络",
                capacity,
                42L,
                status,
                null,
                LocalDateTime.of(2026, 3, 1, 8, 0),
                LocalDateTime.of(2026, 9, 1, 9, 30)
        );
    }
}
