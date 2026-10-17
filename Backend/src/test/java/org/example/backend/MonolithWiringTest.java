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

    /**
     * applicationUsesThe根目录PackageAndDoesNotImportModule主机的处理逻辑。
     */
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

    /**
     * featureReadAndWriteRoutesHaveOneController所有者的处理逻辑。
     */
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

    /**
     * legacyWrite数据映射器Remains可用AlongsideFeature数据映射器的处理逻辑。
     */
    @Test
    void legacyWriteMapperRemainsAvailableAlongsideFeatureMappers() {
        assertNotNull(ClassTimetableMapper.class.getAnnotation(Mapper.class));
    }

    /**
     * 目标FeaturesDoNotRetainSingleImplementationForwarding端口的处理逻辑。
     */
    @Test
    void targetFeaturesDoNotRetainSingleImplementationForwardingPorts() {
        Path services = mainJava().resolve("org/example/backend/service");
        FORWARDING_PORTS.forEach(/** 处理遍历到的当前条目。 入参：name。 */ name -> assertFalse(Files.exists(services.resolve(name)), name));
    }

    /**
     * ordinaryFeature服务UseComponentScanning的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void ordinaryFeatureServicesUseComponentScanning() throws IOException {
        assertNotNull(LabRecordReadService.class.getAnnotation(Service.class));
        assertNotNull(TeachingScheduleReadService.class.getAnnotation(Service.class));
        assertNotNull(ReservationPersistenceService.class.getAnnotation(Service.class));
        assertNotNull(ReservationLabService.class.getAnnotation(Service.class));
        assertNotNull(ReservationScheduleService.class.getAnnotation(Service.class));
        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertNotNull(DefaultLabCatalogQuery.class.getAnnotation(Service.class)),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertNotNull(DefaultLabAvailabilityQuery.class.getAnnotation(Service.class)),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertNotNull(DefaultTeachingScheduleQuery.class.getAnnotation(Service.class)),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertNotNull(DefaultScheduleOccupancyQuery.class.getAnnotation(Service.class)),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertNotNull(DefaultReservationCatalogQuery.class.getAnnotation(Service.class)),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertNotNull(DefaultReservationAvailabilityEvaluator.class.getAnnotation(Service.class)),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertNotNull(DefaultReservationCreationService.class.getAnnotation(Service.class)),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertNotNull(DefaultReservationReviewService.class.getAnnotation(Service.class)),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertNotNull(DefaultReservationCommandHandler.class.getAnnotation(Service.class))
        );

        String configuration = Files.readString(mainJava().resolve(
                "org/example/backend/config/FeatureConfiguration.java"));
        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("labRecordReadPort(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("teachingScheduleReadPort(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("reservationPersistencePort(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("labAvailabilityPort(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("managedLaboratoryIdsPort(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("laboratorySummaryPort(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("scheduleConflictPort(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("labCatalogQuery(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("labAvailabilityQuery(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("teachingScheduleQuery(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("scheduleOccupancyQuery(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("reservationCatalogQuery(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("reservationAvailabilityEvaluator(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("reservationCreationService(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("reservationReviewService(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(configuration.contains("reservationCommandHandler("))
        );
    }

    /**
     * mainJava的处理逻辑。
     * @return Path 类型的处理结果。
     */
    private Path mainJava() {
        Path workingDirectory = Path.of(System.getProperty("user.dir"));
        Path repositorySource = workingDirectory.resolve("Backend/src/main/java");
        if (Files.isDirectory(repositorySource)) {
            return repositorySource;
        }
        return workingDirectory.resolve("src/main/java");
    }

    /**
     * 统计GetMappings。
     * @param path 路径参数。
     * @param controller controller参数。
     * @return long 类型的处理结果。
     */
    private long countGetMappings(String path, Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .map(/** 将当前条目转换为目标结果。 入参：method。 */ method -> method.getAnnotation(GetMapping.class))
                .filter(/** 判断当前条目是否符合筛选条件。 入参：mapping。 */ mapping -> mapping != null)
                .flatMap(/** 转换并展开当前条目。 入参：mapping。 */ mapping -> Arrays.stream(mapping.value()))
                .filter(path::equals)
                .count();
    }

    /**
     * 统计PostMappings。
     * @param path 路径参数。
     * @param controller controller参数。
     * @return long 类型的处理结果。
     */
    private long countPostMappings(String path, Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .map(/** 将当前条目转换为目标结果。 入参：method。 */ method -> method.getAnnotation(PostMapping.class))
                .filter(/** 判断当前条目是否符合筛选条件。 入参：mapping。 */ mapping -> mapping != null)
                .flatMap(/** 转换并展开当前条目。 入参：mapping。 */ mapping -> Arrays.stream(mapping.value()))
                .filter(path::equals)
                .count();
    }
}
