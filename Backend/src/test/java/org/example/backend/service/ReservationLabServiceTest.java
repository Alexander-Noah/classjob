package org.example.backend.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationLabServiceTest {
    private static final TimeRange RANGE = new TimeRange(
            Instant.parse("2026-10-12T01:00:00Z"),
            Instant.parse("2026-10-12T03:00:00Z")
    );

    @Test
    void propagatesUnavailableLaboratoryMessage() {
        var service = service(List.of(), (labId, capacity, startsAt, endsAt) ->
                CapabilityResult.unavailable("laboratory data is unavailable"));

        CapabilityResult<LabAvailabilitySnapshot> result = service.checkAvailability(7L, 30, RANGE);

        assertEquals(CapabilityStatus.UNAVAILABLE, result.status());
        assertEquals("laboratory data is unavailable", result.message());
    }

    @Test
    void mapsAvailableLaboratorySnapshot() {
        var service = service(List.of(), (labId, capacity, startsAt, endsAt) ->
                CapabilityResult.available(new LabAvailabilityView(labId, 48, true)));

        CapabilityResult<LabAvailabilitySnapshot> result = service.checkAvailability(7L, 30, RANGE);

        assertEquals(CapabilityResult.available(new LabAvailabilitySnapshot(7L, 48, true)), result);
    }

    @Test
    void rejectsDuplicateManagedLaboratoryIds() {
        var service = service(List.of(lab(7L, "LAB-7", "Lab Seven"), lab(7L, "OTHER", "Other")), availableLab());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.findManagedLaboratoryIds(101L)
        );

        assertEquals("lab catalog returned a duplicate laboratory id", exception.getMessage());
    }

    @Test
    void rejectsDuplicateLaboratorySummaryIds() {
        var service = service(List.of(lab(7L, "LAB-7", "Lab Seven"), lab(7L, "OTHER", "Other")), availableLab());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.findLaboratorySummaries(Set.of(7L))
        );

        assertEquals("lab catalog returned a duplicate laboratory id", exception.getMessage());
    }

    @Test
    void rejectsNullAndInvalidLaboratoryIdSetsWithExistingMessages() {
        var service = service(List.of(), availableLab());
        Set<Long> idsWithNull = new HashSet<>();
        idsWithNull.add(null);

        IllegalArgumentException missing = assertThrows(
                IllegalArgumentException.class,
                () -> service.findLaboratorySummaries(null)
        );
        IllegalArgumentException nullId = assertThrows(
                IllegalArgumentException.class,
                () -> service.findLaboratorySummaries(idsWithNull)
        );
        IllegalArgumentException invalid = assertThrows(
                IllegalArgumentException.class,
                () -> service.findLaboratorySummaries(Set.of(0L))
        );

        assertEquals("laboratory ids are required", missing.getMessage());
        assertEquals("laboratory ids must be positive", nullId.getMessage());
        assertEquals("laboratory ids must be positive", invalid.getMessage());
    }

    @Test
    void returnsRequestedLaboratorySummariesOnly() {
        var service = service(
                List.of(lab(7L, "LAB-7", "Lab Seven"), lab(8L, "LAB-8", "Lab Eight")),
                availableLab()
        );

        Map<Long, LaboratorySummary> summaries = service.findLaboratorySummaries(Set.of(8L));

        assertEquals(Map.of(8L, new LaboratorySummary("Lab Eight", "LAB-8")), summaries);
    }

    private static ReservationLabService service(List<LabView> labs, LabAvailabilityQuery availability) {
        return new ReservationLabService(new StubCatalog(labs), availability);
    }

    private static LabAvailabilityQuery availableLab() {
        return (labId, capacity, startsAt, endsAt) ->
                CapabilityResult.available(new LabAvailabilityView(labId, capacity, true));
    }

    private static LabView lab(Long id, String code, String name) {
        return new LabView(id, code, name, null, null, null, null, 50, null, null, null, 0, null, null, null);
    }

    private record StubCatalog(List<LabView> labs) implements LabCatalogQuery {
        @Override
        public List<LabView> list(LabAccessScope scope) {
            return labs;
        }

        @Override
        public LabView findById(long id, LabAccessScope scope) {
            return labs.stream().filter(lab -> Long.valueOf(id).equals(lab.id())).findFirst().orElse(null);
        }
    }
}
