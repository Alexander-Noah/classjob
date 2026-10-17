/*
 * MonolithWiringTest回归测试；测试夹具与生产配置分离。
 * 覆盖场景：applicationUsesTheRootPackageAndDoesNotImportModuleHosts、featureReadAndWriteRoutesHaveOneControllerOwner、legacyWriteMapperRemainsAvailableAlongsideFeatureMappers、targetFeaturesDoNotRetainSingleImplementationForwardingPorts 等。
 */
package org.example.backend;

import org.apache.ibatis.annotations.Mapper;
import org.example.backend.controller.LabQueryController;
import org.example.backend.controller.ReservationController;
import org.example.backend.controller.TeachingScheduleQueryController;
import org.example.backend.mapper.ClassTimetableMapper;
import org.example.backend.service.DefaultLabAvailabilityQuery;
import org.example.backend.service.DefaultLabCatalogQuery;
import org.example.backend.service.DefaultReservationAvailabilityEvaluator;
import org.example.backend.service.DefaultReservationCatalogQuery;
import org.example.backend.service.DefaultReservationCommandHandler;
import org.example.backend.service.DefaultReservationCreationService;
import org.example.backend.service.DefaultReservationReviewService;
import org.example.backend.service.DefaultScheduleOccupancyQuery;
import org.example.backend.service.DefaultTeachingScheduleQuery;
import org.example.backend.service.LabRecordReadService;
import org.example.backend.service.ReservationLabService;
import org.example.backend.service.ReservationPersistenceService;
import org.example.backend.service.ReservationScheduleService;
import org.example.backend.service.TeachingScheduleReadService;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MonolithWiringTest {
    private static final Set<String> FORWARDING_PORTS = Set.of(
            "LabRecordReadPort.java",
            "TeachingScheduleReadPort.java",
            "ReservationPersistencePort.java",
            "LabAvailabilityPort.java",
            "ManagedLaboratoryIdsPort.java",
            "LaboratorySummaryPort.java",
            "ScheduleConflictPort.java"
    );

    @Test
    void applicationUsesTheRootPackageAndDoesNotImportModuleHosts() {
        SpringBootApplication application = BackendApplication.class.getAnnotation(SpringBootApplication.class);
        assertNotNull(application);
        assertEquals(Set.of(), Set.of(application.scanBasePackages()));
        assertFalse(BackendApplication.class.isAnnotationPresent(Import.class));

        MapperScan mapperScan = BackendApplication.class.getAnnotation(MapperScan.class);
        assertNotNull(mapperScan);
        assertEquals(Set.of("org.example.backend.mapper"), Set.of(mapperScan.basePackages()));
    }

    @Test
    void featureReadAndWriteRoutesHaveOneControllerOwner() {
        Class<?> legacyLabController = org.example.backend.controller.LabController.class;
        Class<?> labQueryController = LabQueryController.class;
        assertEquals(1, countGetMappings("/labs", labQueryController));
        assertEquals(1, countGetMappings("/labs/{id:\\d+}", labQueryController));
        assertEquals(1, countGetMappings("/labs/{id:\\d+}/devices", legacyLabController));

        Class<?> legacyTimetableController = org.example.backend.controller.ClassTimetableController.class;
        assertEquals(1, countGetMappings("/class-timetables", TeachingScheduleQueryController.class));
        assertEquals(1, countGetMappings("/class-timetables/crawler-config", legacyTimetableController));

        Class<?> legacyReservationController = org.example.backend.controller.ReservationsController.class;
        assertEquals(1, countGetMappings("/reservations", ReservationController.class));
        assertEquals(1, countPostMappings("/reservations", ReservationController.class));
        assertEquals(1, countPostMappings("/reservations/{id:\\d+}/approve", ReservationController.class));
        assertEquals(1, countGetMappings("/public/labs/{id:\\d+}/reservation-profile", legacyReservationController));
    }

    @Test
    void legacyWriteMapperRemainsAvailableAlongsideFeatureMappers() {
        assertNotNull(ClassTimetableMapper.class.getAnnotation(Mapper.class));
    }

    @Test
    void targetFeaturesDoNotRetainSingleImplementationForwardingPorts() {
        Path services = mainJava().resolve("org/example/backend/service");
        FORWARDING_PORTS.forEach(name -> assertFalse(Files.exists(services.resolve(name)), name));
    }

    @Test
    void ordinaryFeatureServicesUseComponentScanning() throws IOException {
        assertNotNull(LabRecordReadService.class.getAnnotation(Service.class));
        assertNotNull(TeachingScheduleReadService.class.getAnnotation(Service.class));
        assertNotNull(ReservationPersistenceService.class.getAnnotation(Service.class));
        assertNotNull(ReservationLabService.class.getAnnotation(Service.class));
        assertNotNull(ReservationScheduleService.class.getAnnotation(Service.class));
        assertAll(
                () -> assertNotNull(DefaultLabCatalogQuery.class.getAnnotation(Service.class)),
                () -> assertNotNull(DefaultLabAvailabilityQuery.class.getAnnotation(Service.class)),
                () -> assertNotNull(DefaultTeachingScheduleQuery.class.getAnnotation(Service.class)),
                () -> assertNotNull(DefaultScheduleOccupancyQuery.class.getAnnotation(Service.class)),
                () -> assertNotNull(DefaultReservationCatalogQuery.class.getAnnotation(Service.class)),
                () -> assertNotNull(DefaultReservationAvailabilityEvaluator.class.getAnnotation(Service.class)),
                () -> assertNotNull(DefaultReservationCreationService.class.getAnnotation(Service.class)),
                () -> assertNotNull(DefaultReservationReviewService.class.getAnnotation(Service.class)),
                () -> assertNotNull(DefaultReservationCommandHandler.class.getAnnotation(Service.class))
        );

        String configuration = Files.readString(mainJava().resolve(
                "org/example/backend/config/FeatureConfiguration.java"));
        assertAll(
                () -> assertFalse(configuration.contains("labRecordReadPort(")),
                () -> assertFalse(configuration.contains("teachingScheduleReadPort(")),
                () -> assertFalse(configuration.contains("reservationPersistencePort(")),
                () -> assertFalse(configuration.contains("labAvailabilityPort(")),
                () -> assertFalse(configuration.contains("managedLaboratoryIdsPort(")),
                () -> assertFalse(configuration.contains("laboratorySummaryPort(")),
                () -> assertFalse(configuration.contains("scheduleConflictPort(")),
                () -> assertFalse(configuration.contains("labCatalogQuery(")),
                () -> assertFalse(configuration.contains("labAvailabilityQuery(")),
                () -> assertFalse(configuration.contains("teachingScheduleQuery(")),
                () -> assertFalse(configuration.contains("scheduleOccupancyQuery(")),
                () -> assertFalse(configuration.contains("reservationCatalogQuery(")),
                () -> assertFalse(configuration.contains("reservationAvailabilityEvaluator(")),
                () -> assertFalse(configuration.contains("reservationCreationService(")),
                () -> assertFalse(configuration.contains("reservationReviewService(")),
                () -> assertFalse(configuration.contains("reservationCommandHandler("))
        );
    }

    private Path mainJava() {
        Path workingDirectory = Path.of(System.getProperty("user.dir"));
        Path repositorySource = workingDirectory.resolve("Backend/src/main/java");
        if (Files.isDirectory(repositorySource)) {
            return repositorySource;
        }
        return workingDirectory.resolve("src/main/java");
    }

    private long countGetMappings(String path, Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .map(method -> method.getAnnotation(GetMapping.class))
                .filter(mapping -> mapping != null)
                .flatMap(mapping -> Arrays.stream(mapping.value()))
                .filter(path::equals)
                .count();
    }

    private long countPostMappings(String path, Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .map(method -> method.getAnnotation(PostMapping.class))
                .filter(mapping -> mapping != null)
                .flatMap(mapping -> Arrays.stream(mapping.value()))
                .filter(path::equals)
                .count();
    }
}
