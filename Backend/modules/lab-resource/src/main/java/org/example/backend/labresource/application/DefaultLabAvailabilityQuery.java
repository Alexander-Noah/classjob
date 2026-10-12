package org.example.backend.labresource.application;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.labresource.api.LabAvailabilityQuery;
import org.example.backend.labresource.api.LabAvailabilityView;
import org.example.backend.labresource.application.LabRecord;
import org.example.backend.labresource.application.LabRecordReadPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public final class DefaultLabAvailabilityQuery implements LabAvailabilityQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLabAvailabilityQuery.class);
    private static final Set<String> ELIGIBLE_STATUSES = Set.of("开放", "预约中", "使用中");
    private static final Set<String> KNOWN_INELIGIBLE_STATUSES = Set.of("维护中", "停用");

    private final LabRecordReadPort records;

    public DefaultLabAvailabilityQuery(LabRecordReadPort records) {
        this.records = Objects.requireNonNull(records, "records");
    }

    @Override
    public CapabilityResult<LabAvailabilityView> check(
            Long labId,
            int expectedCapacity,
            Instant startsAt,
            Instant endsAt
    ) {
        validate(labId, expectedCapacity, startsAt, endsAt);
        LabRecord record = records.findById(labId, null);
        if (record == null) {
            return CapabilityResult.available(new LabAvailabilityView(labId, 0, false));
        }

        int capacity = record.capacity() == null ? 0 : Math.max(record.capacity(), 0);
        String status = record.openStatus() == null ? "" : record.openStatus().trim();
        boolean eligibleStatus = ELIGIBLE_STATUSES.contains(status);
        if (!eligibleStatus && !KNOWN_INELIGIBLE_STATUSES.contains(status)) {
            LOGGER.warn("Unknown lab open status; labId={}, status={}", labId, status);
        }
        return CapabilityResult.available(new LabAvailabilityView(
                labId,
                capacity,
                eligibleStatus && capacity >= expectedCapacity
        ));
    }

    private void validate(Long labId, int expectedCapacity, Instant startsAt, Instant endsAt) {
        if (labId == null || labId <= 0) {
            throw new IllegalArgumentException("labId must be positive");
        }
        if (expectedCapacity <= 0) {
            throw new IllegalArgumentException("expectedCapacity must be positive");
        }
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("time range must have a start before its end");
        }
    }
}
