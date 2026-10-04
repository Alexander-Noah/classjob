package org.example.backend;

import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplicitModuleWiringTest {
    private static final String LEGACY_CONTROLLER =
            "org.example.backend.controller.ClassTimetableController";
    private static final String MODULE_CONTROLLER =
            "org.example.backend.teachingschedule.adapter.in.web.TeachingScheduleQueryController";
    private static final String LEGACY_LAB_CONTROLLER =
            "org.example.backend.controller.LabController";
    private static final String MODULE_LAB_CONTROLLER =
            "org.example.backend.labresource.adapter.in.web.LabQueryController";
    private static final String LEGACY_RESERVATION_CONTROLLER =
            "org.example.backend.controller.ReservationsController";
    private static final String MODULE_RESERVATION_CONTROLLER =
            "org.example.backend.reservation.adapter.in.web.ReservationController";

    @Test
    void hostUsesExplicitLegacyScanAndImportsModuleRuntimeConfiguration() {
        SpringBootApplication application = BackendApplication.class.getAnnotation(SpringBootApplication.class);
        assertNotNull(application);
        assertEquals(Set.of(
                "org.example.backend.config",
                "org.example.backend.controller",
                "org.example.backend.security",
                "org.example.backend.service",
                "org.example.backend.websocket"
        ), Set.of(application.scanBasePackages()));

        Import imported = BackendApplication.class.getAnnotation(Import.class);
        assertNotNull(imported);
        Set<String> importedTypes = Arrays.stream(imported.value())
                .map(Class::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(importedTypes.contains("org.example.backend.kernel.bootstrap.KernelConfiguration"));
        assertTrue(importedTypes.contains(
                "org.example.backend.teachingschedule.bootstrap.TeachingScheduleRuntimeConfiguration"
        ));
        assertTrue(importedTypes.contains(
                "org.example.backend.labresource.bootstrap.LabResourceRuntimeConfiguration"
        ));
        assertTrue(importedTypes.contains(
                "org.example.backend.bridge.reservationlab.ReservationLabResourceBridgeConfiguration"
        ));
        assertTrue(importedTypes.contains(
                "org.example.backend.bridge.reservationschedule.ReservationTeachingScheduleBridgeConfiguration"
        ));
        assertTrue(importedTypes.contains(
                "org.example.backend.reservation.bootstrap.ReservationPersistenceRuntimeConfiguration"
        ));

        MapperScan mapperScan = BackendApplication.class.getAnnotation(MapperScan.class);
        assertNotNull(mapperScan);
        assertEquals(Set.of("org.example.backend.mapper"), Set.of(mapperScan.basePackages()));
    }

    @Test
    void labReadRoutesHaveOneModuleOwnerWhileWriteAndDeviceRoutesStayLegacy() throws Exception {
        Class<?> legacyController = Class.forName(LEGACY_LAB_CONTROLLER);
        Class<?> moduleController = Class.forName(MODULE_LAB_CONTROLLER);

        assertEquals(1, countGetMappings("/labs", legacyController, moduleController));
        assertEquals(1, countGetMappings("/labs/{id:\\d+}", legacyController, moduleController));
        assertEquals(1, countGetMappings("/labs/{id:\\d+}/devices", legacyController, moduleController));
        assertEquals(0, countGetMappings("/labs/{id:\\d+}/devices", moduleController));
    }

    @Test
    void readRoutesHaveOneModuleOwnerWhileCrawlerRoutesStayLegacy() throws Exception {
        Class<?> legacyController = Class.forName(LEGACY_CONTROLLER);
        Class<?> moduleController = Class.forName(MODULE_CONTROLLER);

        assertEquals(1, countGetMappings("/class-timetables", legacyController, moduleController));
        assertEquals(1, countGetMappings("/class-timetables/summary", legacyController, moduleController));
        assertEquals(1, countGetMappings("/class-timetables/semesters", legacyController, moduleController));
        assertEquals(1, countGetMappings("/class-timetables/crawler-config", legacyController, moduleController));
        assertEquals(0, countGetMappings("/class-timetables/crawler-config", moduleController));
    }

    @Test
    void coreReservationRoutesHaveOneModuleOwnerWhileNonMvpRoutesStayLegacy() throws Exception {
        Class<?> legacyController = Class.forName(LEGACY_RESERVATION_CONTROLLER);
        Class<?> moduleController = Class.forName(MODULE_RESERVATION_CONTROLLER);

        assertEquals(1, countGetMappings("/reservations", legacyController, moduleController));
        assertEquals(1, countGetMappings("/reservations/{id:\\d+}", legacyController, moduleController));
        assertEquals(1, countPostMappings("/reservations", legacyController, moduleController));
        assertEquals(1, countPostMappings("/reservations/{id:\\d+}/approve", legacyController, moduleController));
        assertEquals(1, countPostMappings("/reservations/{id:\\d+}/reject", legacyController, moduleController));

        assertEquals(1, countGetMappings("/public/labs/{id:\\d+}/reservation-profile", legacyController, moduleController));
        assertEquals(1, countPostMappings("/public/reservations/scan", legacyController, moduleController));
        assertEquals(1, countGetMappings("/public/reservations/{id:\\d+}/status", legacyController, moduleController));
        assertEquals(1, countPostMappings("/reservations/reset", legacyController, moduleController));
        assertEquals(0, countGetMappings("/public/labs/{id:\\d+}/reservation-profile", moduleController));
        assertEquals(0, countPostMappings("/public/reservations/scan", moduleController));
        assertEquals(0, countPostMappings("/reservations/reset", moduleController));
    }

    @Test
    void legacyMapperRemainsAWriteSideMapperWithoutReadQueries() throws Exception {
        Class<?> mapper = Class.forName("org.example.backend.mapper.ClassTimetableMapper");
        assertNotNull(mapper.getAnnotation(Mapper.class));
        Set<String> methodNames = Arrays.stream(mapper.getDeclaredMethods())
                .map(Method::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(methodNames.contains("insertTimetable"));
        assertTrue(methodNames.contains("deleteBySemester"));
        assertTrue(Stream.of("listTimetables", "getSummary", "listSemesters")
                .noneMatch(methodNames::contains));
    }

    private long countGetMappings(String path, Class<?>... controllers) {
        return Arrays.stream(controllers)
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods()))
                .map(method -> method.getAnnotation(GetMapping.class))
                .filter(mapping -> mapping != null)
                .flatMap(mapping -> Arrays.stream(mapping.value()))
                .filter(path::equals)
                .count();
    }

    private long countPostMappings(String path, Class<?>... controllers) {
        return Arrays.stream(controllers)
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods()))
                .map(method -> method.getAnnotation(PostMapping.class))
                .filter(mapping -> mapping != null)
                .flatMap(mapping -> Arrays.stream(mapping.value()))
                .filter(path::equals)
                .count();
    }
}
