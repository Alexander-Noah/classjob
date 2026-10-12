package org.example.backend.bridge.reservationlab;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.labresource.api.LabAccessScope;
import org.example.backend.labresource.api.LabAvailabilityQuery;
import org.example.backend.labresource.api.LabAvailabilityView;
import org.example.backend.labresource.api.LabCatalogQuery;
import org.example.backend.labresource.api.LabView;
import org.example.backend.reservation.application.LabAvailabilityPort;
import org.example.backend.reservation.domain.TimeRange;
import org.example.backend.reservation.application.LaboratorySummaryPort;
import org.example.backend.reservation.application.ManagedLaboratoryIdsPort;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationLabResourceBridgeConfigurationTest {
    @Test
    void mapsProviderViewIntoConsumerOwnedSnapshot() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                ProviderFixture.class,
                CatalogFixture.class,
                ReservationLabResourceBridgeConfiguration.class
        )) {
            TimeRange range = new TimeRange(Instant.parse("2026-10-01T01:00:00Z"), Instant.parse("2026-10-01T03:00:00Z"));
            var result = context.getBean(LabAvailabilityPort.class).check(9L, 30, range);

            assertEquals(CapabilityStatus.AVAILABLE, result.status());
            assertEquals(9L, result.value().labId());
            assertEquals(60, result.value().capacity());
        }
    }

    @Test
    void returnsOnlyPositiveManagedLaboratoryIds() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                ProviderFixture.class,
                ManagedLabsFixture.class,
                ReservationLabResourceBridgeConfiguration.class
        )) {
            assertEquals(Set.of(3L, 7L), context.getBean(ManagedLaboratoryIdsPort.class).findByManagerUserId(42L));
        }
    }

    @Test
    void mapsExactlyRequestedLaboratoriesByIdRegardlessOfProviderOrder() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                ProviderFixture.class,
                ShuffledLabsFixture.class,
                ReservationLabResourceBridgeConfiguration.class
        )) {
            Map<Long, org.example.backend.reservation.application.LaboratorySummary> summaries = context
                    .getBean(LaboratorySummaryPort.class)
                    .findByIds(Set.of(3L, 7L));

            assertEquals(Map.of(
                    3L, new org.example.backend.reservation.application.LaboratorySummary("材料实验室", "MAT-3"),
                    7L, new org.example.backend.reservation.application.LaboratorySummary("电子实验室", "ELE-7")
            ), summaries);
        }
    }

    @Test
    void shortCircuitsEmptyLaboratoryRequestsWithoutQueryingProvider() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                ProviderFixture.class,
                EmptyRequestFixture.class,
                ReservationLabResourceBridgeConfiguration.class
        )) {
            assertEquals(Map.of(), context.getBean(LaboratorySummaryPort.class).findByIds(Set.of()));
        }
    }

    @Test
    void rejectsDuplicateLaboratoryIdsInsteadOfOverwritingOne() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                ProviderFixture.class,
                DuplicateLabsFixture.class,
                ReservationLabResourceBridgeConfiguration.class
        )) {
            assertThrows(IllegalStateException.class, () -> context.getBean(LaboratorySummaryPort.class).findByIds(Set.of(3L)));
        }
    }

    @Test
    void ignoresNormalUnrequestedLaboratoriesWhenListingTheFullCatalog() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                ProviderFixture.class,
                OutsideRequestedLabsFixture.class,
                ReservationLabResourceBridgeConfiguration.class
        )) {
            assertEquals(Map.of(
                    3L, new org.example.backend.reservation.application.LaboratorySummary("材料实验室", "MAT-3")
            ), context.getBean(LaboratorySummaryPort.class).findByIds(Set.of(3L)));
        }
    }

    @Test
    void rejectsNullCatalogResultsInsteadOfInventingLaboratoryContext() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                ProviderFixture.class,
                NullCatalogFixture.class,
                ReservationLabResourceBridgeConfiguration.class
        )) {
            assertThrows(IllegalStateException.class, () -> context.getBean(LaboratorySummaryPort.class).findByIds(Set.of(3L)));
        }
    }

    @Test
    void rejectsMalformedCatalogRecordsInsteadOfSkippingThem() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                ProviderFixture.class,
                MalformedCatalogFixture.class,
                ReservationLabResourceBridgeConfiguration.class
        )) {
            assertThrows(IllegalStateException.class, () -> context.getBean(LaboratorySummaryPort.class).findByIds(Set.of(3L)));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ProviderFixture {
        @Bean
        LabAvailabilityQuery labAvailabilityQuery() {
            return (labId, expectedCapacity, startsAt, endsAt) -> CapabilityResult.available(new LabAvailabilityView(labId, 60, true));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CatalogFixture {
        @Bean
        LabCatalogQuery labCatalogQuery() {
            return catalog(List.of());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ManagedLabsFixture {
        @Bean
        LabCatalogQuery labCatalogQuery() {
            return new LabCatalogQuery() {
                @Override
                public List<LabView> list(LabAccessScope scope) {
                    assertEquals(LabAccessScope.managedBy(42L), scope);
                    return List.of(lab(3L, "MAT-3", "材料实验室"), lab(0L, "IGNORED", "忽略"), lab(7L, "ELE-7", "电子实验室"));
                }

                @Override
                public LabView findById(long id, LabAccessScope scope) {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ShuffledLabsFixture {
        @Bean
        LabCatalogQuery labCatalogQuery() {
            return catalog(List.of(lab(7L, "ELE-7", "电子实验室"), lab(3L, "MAT-3", "材料实验室")));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class EmptyRequestFixture {
        @Bean
        LabCatalogQuery labCatalogQuery() {
            return catalog(null);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DuplicateLabsFixture {
        @Bean
        LabCatalogQuery labCatalogQuery() {
            return catalog(List.of(lab(3L, "MAT-3", "材料实验室"), lab(3L, "MAT-03", "另一个材料实验室")));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OutsideRequestedLabsFixture {
        @Bean
        LabCatalogQuery labCatalogQuery() {
            return catalog(List.of(lab(3L, "MAT-3", "材料实验室"), lab(7L, "ELE-7", "电子实验室")));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class NullCatalogFixture {
        @Bean
        LabCatalogQuery labCatalogQuery() {
            return catalog(null);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MalformedCatalogFixture {
        @Bean
        LabCatalogQuery labCatalogQuery() {
            return catalog(java.util.Collections.singletonList(null));
        }
    }

    private static LabCatalogQuery catalog(List<LabView> labs) {
        return new LabCatalogQuery() {
            @Override
            public List<LabView> list(LabAccessScope scope) {
                assertEquals(LabAccessScope.all(), scope);
                return labs;
            }

            @Override
            public LabView findById(long id, LabAccessScope scope) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static LabView lab(Long id, String code, String name) {
        return new LabView(id, code, name, null, null, null, null, null, null, null, null, 0, null, null, null);
    }
}
