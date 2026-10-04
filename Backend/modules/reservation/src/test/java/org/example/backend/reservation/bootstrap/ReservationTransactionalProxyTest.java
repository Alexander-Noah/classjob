package org.example.backend.reservation.bootstrap;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.reservation.api.ReservationAccessScope;
import org.example.backend.reservation.api.ReservationActor;
import org.example.backend.reservation.api.ReservationCatalogQuery;
import org.example.backend.reservation.api.ReservationView;
import org.example.backend.reservation.application.CreateReservationCommand;
import org.example.backend.reservation.application.DefaultReservationCatalogQuery;
import org.example.backend.reservation.application.DefaultReservationCreationService;
import org.example.backend.reservation.application.DefaultReservationReviewService;
import org.example.backend.reservation.application.LabAvailabilityPort;
import org.example.backend.reservation.application.LabAvailabilitySnapshot;
import org.example.backend.reservation.application.ScheduleConflictPort;
import org.example.backend.reservation.application.ScheduleConflictSnapshot;
import org.example.backend.reservation.application.port.out.LaboratorySummary;
import org.example.backend.reservation.application.port.out.LaboratorySummaryPort;
import org.example.backend.reservation.application.port.out.ManagedLaboratoryIdsPort;
import org.example.backend.reservation.application.port.out.NewReservationRecord;
import org.example.backend.reservation.application.port.out.ReservationOverlapCheck;
import org.example.backend.reservation.application.port.out.ReservationPersistencePort;
import org.example.backend.reservation.application.port.out.ReservationRecord;
import org.example.backend.reservation.application.port.out.ReservationRecordScope;
import org.example.backend.reservation.application.port.out.ReservationReviewUpdate;
import org.example.backend.reservation.domain.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationTransactionalProxyTest {
    @Test
    void transactionalReservationServicesAreClassProxiedAndIntercepted() {
        try (var context = new AnnotationConfigApplicationContext(TransactionProxyConfiguration.class)) {
            DefaultReservationCreationService creation = context.getBean(DefaultReservationCreationService.class);
            DefaultReservationReviewService review = context.getBean(DefaultReservationReviewService.class);
            DefaultReservationCatalogQuery catalog = context.getBean(DefaultReservationCatalogQuery.class);
            TrackingTransactionManager transactions = context.getBean(TrackingTransactionManager.class);

            assertTrue(AopUtils.isCglibProxy(creation));
            assertTrue(AopUtils.isCglibProxy(review));
            assertTrue(AopUtils.isCglibProxy(catalog));
            assertTrue(hasTransactionAdvisor(creation));
            assertTrue(hasTransactionAdvisor(review));
            assertTrue(hasTransactionAdvisor(catalog));

            creation.create(command(), ReservationActor.teacher(7L));
            catalog.list(ReservationAccessScope.forActor(ReservationActor.teacher(7L)));

            assertTrue(transactions.started > 0);
            assertTrue(transactions.committed > 0);
        }
    }

    private boolean hasTransactionAdvisor(Object bean) {
        return Arrays.stream(((Advised) bean).getAdvisors())
                .anyMatch(advisor -> advisor.getAdvice() instanceof TransactionInterceptor);
    }

    private CreateReservationCommand command() {
        return new CreateReservationCommand("王老师", "计算机学院", "13800000000", 7L,
                LocalDate.of(2026, 10, 5), "09:00-11:00", "课程实验", 30, "课程使用");
    }

    private static ReservationRecord insertedRecord() {
        return new ReservationRecord(1L, 7L, "王老师", "计算机学院", "13800000000", 7L,
                LocalDate.of(2026, 10, 5), "09:00-11:00", "课程实验", 30, "课程使用",
                ReservationStatus.PENDING, false, "待审核", null,
                LocalDateTime.of(2026, 10, 1, 9, 0), LocalDateTime.of(2026, 10, 1, 9, 0));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class TransactionProxyConfiguration {
        @Bean
        TrackingTransactionManager transactionManager() {
            return new TrackingTransactionManager();
        }

        @Bean
        ReservationPersistencePort reservationPersistencePort() {
            return new ReservationPersistencePort() {
                @Override public List<ReservationRecord> findAll(ReservationRecordScope scope) { return List.of(); }
                @Override public Optional<ReservationRecord> findOne(long id, ReservationRecordScope scope) { return Optional.of(insertedRecord()); }
                @Override public long insert(NewReservationRecord record) { return 1L; }
                @Override public long countActiveOverlaps(ReservationOverlapCheck check) { return 0L; }
                @Override public boolean updateReview(ReservationReviewUpdate update) { return false; }
            };
        }

        @Bean
        ManagedLaboratoryIdsPort managedLaboratoryIdsPort() {
            return userId -> Set.of(7L);
        }

        @Bean
        LaboratorySummaryPort laboratorySummaryPort() {
            return laboratoryIds -> Map.<Long, LaboratorySummary>of();
        }

        @Bean
        LabAvailabilityPort labAvailabilityPort() {
            return (labId, attendees, timeRange) -> CapabilityResult.available(new LabAvailabilitySnapshot(labId, 40, true));
        }

        @Bean
        ScheduleConflictPort scheduleConflictPort() {
            return (labId, date, timeRange) -> CapabilityResult.available(new ScheduleConflictSnapshot(labId, date, timeRange, false));
        }

        @Bean
        DefaultReservationCatalogQuery reservationCatalogQuery(ReservationPersistencePort reservations,
                                                               ManagedLaboratoryIdsPort managedLaboratories,
                                                               LaboratorySummaryPort laboratorySummaries) {
            return new DefaultReservationCatalogQuery(reservations, managedLaboratories, laboratorySummaries);
        }

        @Bean
        DefaultReservationCreationService reservationCreationService(ReservationPersistencePort reservations,
                                                                      ReservationCatalogQuery catalog,
                                                                      ManagedLaboratoryIdsPort managedLaboratories,
                                                                      LabAvailabilityPort laboratoryAvailability,
                                                                      ScheduleConflictPort scheduleConflicts) {
            return new DefaultReservationCreationService(reservations, catalog, managedLaboratories, laboratoryAvailability, scheduleConflicts);
        }

        @Bean
        DefaultReservationReviewService reservationReviewService(ReservationPersistencePort reservations,
                                                                 ReservationCatalogQuery catalog) {
            return new DefaultReservationReviewService(reservations, catalog);
        }
    }

    static final class TrackingTransactionManager implements PlatformTransactionManager {
        private int started;
        private int committed;

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            started++;
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            committed++;
        }

        @Override
        public void rollback(TransactionStatus status) {
            throw new AssertionError("The successful creation/read path must not roll back");
        }
    }
}
