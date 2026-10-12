package org.example.backend.reservation;

import org.example.backend.reservation.web.ReservationController;
import org.example.backend.reservation.infrastructure.MyBatisReservationPersistenceAdapter;
import org.example.backend.reservation.infrastructure.ReservationPersistenceMapper;
import org.example.backend.reservation.api.ReservationCatalogQuery;
import org.example.backend.reservation.application.DefaultReservationCatalogQuery;
import org.example.backend.reservation.application.DefaultReservationAvailabilityEvaluator;
import org.example.backend.reservation.application.DefaultReservationCreationService;
import org.example.backend.reservation.application.DefaultReservationCommandHandler;
import org.example.backend.reservation.application.DefaultReservationReviewService;
import org.example.backend.reservation.application.LabAvailabilityPort;
import org.example.backend.reservation.application.ReservationAvailabilityEvaluator;
import org.example.backend.reservation.application.ScheduleConflictPort;
import org.example.backend.reservation.application.LaboratorySummaryPort;
import org.example.backend.reservation.application.ManagedLaboratoryIdsPort;
import org.example.backend.reservation.application.ReservationPersistencePort;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@MapperScan(basePackageClasses = ReservationPersistenceMapper.class)
@Import(ReservationModuleConfiguration.class)
public class ReservationPersistenceRuntimeConfiguration {
    @Bean
    @ConditionalOnMissingBean(ManagedLaboratoryIdsPort.class)
    public ManagedLaboratoryIdsPort missingManagedLaboratoryIdsPort() {
        return managerUserId -> java.util.Set.of();
    }

    @Bean
    @ConditionalOnMissingBean(LaboratorySummaryPort.class)
    public LaboratorySummaryPort missingLaboratorySummaryPort() {
        return laboratoryIds -> java.util.Map.of();
    }

    @Bean
    public ReservationPersistencePort reservationPersistencePort(ReservationPersistenceMapper mapper) {
        return new MyBatisReservationPersistenceAdapter(mapper);
    }

    @Bean
    public ReservationCatalogQuery reservationCatalogQuery(
            ReservationPersistencePort reservations,
            ManagedLaboratoryIdsPort managedLaboratoryIds,
            LaboratorySummaryPort laboratorySummaries
    ) {
        return new DefaultReservationCatalogQuery(reservations, managedLaboratoryIds, laboratorySummaries);
    }

    @Bean
    public ReservationAvailabilityEvaluator reservationAvailabilityEvaluator(
            ReservationPersistencePort reservations,
            LabAvailabilityPort labAvailability,
            ScheduleConflictPort scheduleConflicts
    ) {
        return new DefaultReservationAvailabilityEvaluator(reservations, labAvailability, scheduleConflicts);
    }

    @Bean
    public DefaultReservationCreationService reservationCreationService(
            ReservationPersistencePort reservations,
            ReservationCatalogQuery catalog,
            ManagedLaboratoryIdsPort managedLaboratoryIds,
            ReservationAvailabilityEvaluator availability
    ) {
        return new DefaultReservationCreationService(reservations, catalog, managedLaboratoryIds, availability);
    }

    @Bean
    public DefaultReservationReviewService reservationReviewService(
            ReservationPersistencePort reservations,
            ReservationCatalogQuery catalog,
            ReservationAvailabilityEvaluator availability
    ) {
        return new DefaultReservationReviewService(reservations, catalog, availability);
    }

    @Bean
    public DefaultReservationCommandHandler reservationCommandHandler(
            DefaultReservationCreationService creation,
            DefaultReservationReviewService review
    ) {
        return new DefaultReservationCommandHandler(creation, review);
    }

    @Bean
    public ReservationController reservationController(
            ReservationCatalogQuery catalog,
            DefaultReservationCommandHandler commands
    ) {
        return new ReservationController(catalog, commands);
    }
}
