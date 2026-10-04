package org.example.backend.reservation.bootstrap;

import org.example.backend.reservation.adapter.in.web.ReservationController;
import org.example.backend.reservation.adapter.out.persistence.MyBatisReservationPersistenceAdapter;
import org.example.backend.reservation.adapter.out.persistence.ReservationPersistenceMapper;
import org.example.backend.reservation.api.ReservationCatalogQuery;
import org.example.backend.reservation.application.DefaultReservationCatalogQuery;
import org.example.backend.reservation.application.DefaultReservationCreationService;
import org.example.backend.reservation.application.DefaultReservationCommandHandler;
import org.example.backend.reservation.application.DefaultReservationReviewService;
import org.example.backend.reservation.application.LabAvailabilityPort;
import org.example.backend.reservation.application.ScheduleConflictPort;
import org.example.backend.reservation.application.port.out.LaboratorySummaryPort;
import org.example.backend.reservation.application.port.out.ManagedLaboratoryIdsPort;
import org.example.backend.reservation.application.port.out.ReservationPersistencePort;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@MapperScan(basePackageClasses = ReservationPersistenceMapper.class)
@Import(ReservationModuleConfiguration.class)
public class ReservationPersistenceRuntimeConfiguration {
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
    public DefaultReservationCreationService reservationCreationService(
            ReservationPersistencePort reservations,
            ReservationCatalogQuery catalog,
            ManagedLaboratoryIdsPort managedLaboratoryIds,
            LabAvailabilityPort labAvailability,
            ScheduleConflictPort scheduleConflicts
    ) {
        return new DefaultReservationCreationService(reservations, catalog, managedLaboratoryIds, labAvailability, scheduleConflicts);
    }

    @Bean
    public DefaultReservationReviewService reservationReviewService(
            ReservationPersistencePort reservations,
            ReservationCatalogQuery catalog
    ) {
        return new DefaultReservationReviewService(reservations, catalog);
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
