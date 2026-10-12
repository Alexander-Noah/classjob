package org.example.backend.labresource.application;

import org.example.backend.labresource.api.LabAccessScope;
import org.example.backend.labresource.application.DeviceCountPort;
import org.example.backend.labresource.application.LabRecord;
import org.example.backend.labresource.application.LabRecordReadPort;
import org.example.backend.labresource.application.ManagerNamePort;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultLabCatalogQueryTest {
    @Test
    void readsBaseAndMetadataInsideOneReadOnlyTransaction() {
        Transactional transaction = DefaultLabCatalogQuery.class.getAnnotation(Transactional.class);

        assertTrue(transaction.readOnly());
    }

    @Test
    void mergesEnrichmentByIdentifiersInsteadOfResultOrder() {
        LabRecord first = lab(1L, 101L, "LAB-001");
        LabRecord second = lab(2L, 202L, "LAB-002");
        LabRecordReadPort records = records(List.of(first, second));
        ManagerNamePort managers = ids -> linkedMap(202L, "Bob", 101L, "Alice");
        DeviceCountPort devices = ids -> linkedMap(2L, 22, 1L, 11);
        var query = new DefaultLabCatalogQuery(records, managers, devices);

        var result = query.list(LabAccessScope.all());

        assertEquals(List.of(1L, 2L), result.stream().map(view -> view.id()).toList());
        assertEquals("Alice", result.get(0).managerName());
        assertEquals(11, result.get(0).deviceCount());
        assertEquals("Bob", result.get(1).managerName());
        assertEquals(22, result.get(1).deviceCount());
    }

    @Test
    void preservesMissingManagerAndZeroDeviceSemantics() {
        var query = new DefaultLabCatalogQuery(
                records(List.of(lab(1L, 101L, "LAB-001"))),
                ids -> Map.of(),
                ids -> Map.of()
        );

        var view = query.list(LabAccessScope.managedBy(101L)).get(0);

        assertNull(view.managerName());
        assertEquals(0, view.deviceCount());
    }

    @Test
    void rejectsDuplicateBaseLabIdentifiers() {
        var query = new DefaultLabCatalogQuery(
                records(List.of(lab(1L, 101L, "LAB-001"), lab(1L, 202L, "LAB-001-copy"))),
                ids -> Map.of(),
                ids -> Map.of()
        );

        assertThrows(IllegalStateException.class, () -> query.list(LabAccessScope.all()));
    }

    @Test
    void rejectsMetadataForLabsOutsideTheRequestedSet() {
        var query = new DefaultLabCatalogQuery(
                records(List.of(lab(1L, 101L, "LAB-001"))),
                ids -> Map.of(101L, "Alice"),
                ids -> Map.of(999L, 4)
        );

        assertThrows(IllegalStateException.class, () -> query.list(LabAccessScope.all()));
    }

    @Test
    void propagatesAdapterFailureInsteadOfReturningPartialDisplayData() {
        RuntimeException failure = new RuntimeException("device metadata unavailable");
        var query = new DefaultLabCatalogQuery(
                records(List.of(lab(1L, 101L, "LAB-001"))),
                ids -> Map.of(101L, "Alice"),
                ids -> { throw failure; }
        );

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> query.list(LabAccessScope.all())
        );

        assertSame(failure, thrown);
    }

    @Test
    void missingDetailDoesNotRequestEnrichment() {
        LabRecordReadPort records = new LabRecordReadPort() {
            @Override
            public List<LabRecord> findAll(Long managerUserId) {
                return List.of();
            }

            @Override
            public LabRecord findById(long id, Long managerUserId) {
                return null;
            }
        };
        var query = new DefaultLabCatalogQuery(
                records,
                ids -> { throw new AssertionError("manager enrichment must not run"); },
                ids -> { throw new AssertionError("device enrichment must not run"); }
        );

        assertNull(query.findById(999L, LabAccessScope.managedBy(42L)));
    }

    @Test
    void detailReadIsRestrictedToTheAuthenticatedManagerScope() {
        var query = new DefaultLabCatalogQuery(
                records(List.of(lab(7L, 42L, "LAB-007"))),
                ids -> Map.of(42L, "Alice"),
                ids -> Map.of(7L, 5)
        );

        assertNull(query.findById(7L, LabAccessScope.managedBy(99L)));
        assertEquals(7L, query.findById(7L, LabAccessScope.managedBy(42L)).id());
    }

    private LabRecordReadPort records(List<LabRecord> records) {
        return new LabRecordReadPort() {
            @Override
            public List<LabRecord> findAll(Long managerUserId) {
                return records;
            }

            @Override
            public LabRecord findById(long id, Long managerUserId) {
                return records.stream()
                        .filter(record -> record.id() == id)
                        .filter(record -> managerUserId == null || managerUserId.equals(record.managerUserId()))
                        .findFirst()
                        .orElse(null);
            }
        };
    }

    private LabRecord lab(long id, Long managerUserId, String code) {
        return new LabRecord(
                id,
                code,
                "Lab " + id,
                "Building A",
                "3",
                "30" + id,
                "Computer",
                48,
                managerUserId,
                "开放",
                "remark",
                LocalDateTime.of(2026, 3, 1, 8, 0),
                LocalDateTime.of(2026, 9, 1, 9, 30)
        );
    }

    private <K, V> Map<K, V> linkedMap(K firstKey, V firstValue, K secondKey, V secondValue) {
        Map<K, V> values = new LinkedHashMap<>();
        values.put(firstKey, firstValue);
        values.put(secondKey, secondValue);
        return values;
    }
}
