/*
 * 维护记录回归测试；测试夹具与生产配置分离。
 * 覆盖场景：maintenanceRecordInterfaceExposesOnlyCrudEndpointsAndFields、serviceRejectsEveryRequiredBlankFieldBeforeWriting、serviceGeneratesRecordNumberAndRequiresResolvedReferences、serviceAllowsNoDeviceAndClearingAnExistingDevice 等。
 */
package org.example.backend.maintenance;

import org.junit.jupiter.api.Test;
import org.example.backend.entity.MaintenanceRecord;
import org.example.backend.mapper.MaintenanceRecordMapper;
import org.example.backend.service.impl.MaintenanceRecordServiceImpl;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaintenanceRecordContractTest {
    private static final List<String> MAINTENANCE_FIELDS = List.of(
            "id", "recordNo", "labId", "deviceId", "lab", "device", "maintenanceType", "maintenanceDate",
            "maintainer", "content", "result", "remark", "updatedAt"
    );

    /**
     * 维护记录InterfaceExposesOnlyCrud端点AndFields的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void maintenanceRecordInterfaceExposesOnlyCrudEndpointsAndFields() throws Exception {
        Class<?> controller = Class.forName("org.example.backend.controller.MaintenanceRecordController");
        Class<?> entity = Class.forName("org.example.backend.entity.MaintenanceRecord");

        assertEquals(
                Set.of(
                        "GET /maintenance-records",
                        "GET /maintenance-records/{id}",
                        "POST /maintenance-records",
                        "PUT /maintenance-records/{id}",
                        "DELETE /maintenance-records/{id}"
                ),
                mappedEndpoints(controller)
        );
        assertEquals(
                MAINTENANCE_FIELDS,
                Arrays.stream(entity.getDeclaredFields()).map(Field::getName).toList()
        );
        for (Method method : controller.getDeclaredMethods()) {
            String methodName = method.getName().toLowerCase();
            for (String forbidden : List.of("assign", "acceptance", "complete", "close", "escalate")) {
                assertFalse(methodName.contains(forbidden), method.getName());
            }
        }
    }

    /**
     * 服务RejectsEveryRequired空白FieldBeforeWriting的处理逻辑。
     */
    @Test
    void serviceRejectsEveryRequiredBlankFieldBeforeWriting() {
        for (String field : List.of(
                "lab", "maintenanceType", "maintenanceDate", "maintainer", "content", "result"
        )) {
            MaintenanceRecord record = validRecord();
            switch (field) {
                case "lab" -> record.setLab(" ");
                case "device" -> record.setDevice(" ");
                case "maintenanceType" -> record.setMaintenanceType(" ");
                case "maintenanceDate" -> record.setMaintenanceDate(" ");
                case "maintainer" -> record.setMaintainer(" ");
                case "content" -> record.setContent(" ");
                case "result" -> record.setResult(" ");
                default -> throw new IllegalStateException(field);
            }

            IllegalArgumentException error = assertThrows(
                    IllegalArgumentException.class,
                    /**
                     * 处理 assertThrows 调用中的回调逻辑。
                     */
                    () -> new MaintenanceRecordServiceImpl(new RecordingMapper()).createMaintenanceRecord(record)
            );
            assertTrue(error.getMessage().startsWith("请填写"));
        }
    }

    /**
     * 服务Generates记录数值AndRequiresResolved引用的处理逻辑。
     */
    @Test
    void serviceGeneratesRecordNumberAndRequiresResolvedReferences() {
        RecordingMapper mapper = new RecordingMapper();
        MaintenanceRecord submitted = validRecord();
        submitted.setRecordNo("client-provided-number");
        MaintenanceRecord created = new MaintenanceRecordServiceImpl(mapper).createMaintenanceRecord(submitted);

        assertTrue(created.getRecordNo().matches("MR-\\d{14}-\\d{4}"));
        assertEquals(11L, mapper.insertedLabId);
        assertEquals(12L, mapper.insertedDeviceId);
        assertEquals(11L, mapper.requestedDeviceLabId);

        mapper.labId = null;
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                /**
                 * 处理 assertThrows 调用中的回调逻辑。
                 */
                () -> new MaintenanceRecordServiceImpl(mapper).createMaintenanceRecord(validRecord())
        );
        assertTrue(error.getMessage().contains("实验室不存在"));
    }

    /**
     * 服务AllowsNo设备AndClearingAnExisting设备的处理逻辑。
     */
    @Test
    void serviceAllowsNoDeviceAndClearingAnExistingDevice() {
        RecordingMapper mapper = new RecordingMapper();
        MaintenanceRecordServiceImpl service = new MaintenanceRecordServiceImpl(mapper);
        MaintenanceRecord record = validRecord();
        record.setDevice(null);
        service.createMaintenanceRecord(record);
        assertEquals(null, mapper.insertedDeviceId);
        assertEquals(null, mapper.requestedDeviceLabId);

        MaintenanceRecord linked = service.createMaintenanceRecord(validRecord());
        MaintenanceRecord cleared = validRecord();
        cleared.setDevice(" ");
        service.updateMaintenanceRecord(linked.getId(), cleared);
        assertEquals(null, mapper.insertedDeviceId);
    }

    /**
     * deletionRemovesThe记录来源NormalReads的处理逻辑。
     */
    @Test
    void deletionRemovesTheRecordFromNormalReads() {
        RecordingMapper mapper = new RecordingMapper();
        MaintenanceRecordServiceImpl service = new MaintenanceRecordServiceImpl(mapper);
        MaintenanceRecord saved = service.createMaintenanceRecord(validRecord());
        service.deleteMaintenanceRecord(saved.getRecordNo());
        assertTrue(mapper.deleteCalled);
        assertTrue(service.listMaintenanceRecords().isEmpty());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                /**
                 * 处理 assertThrows 调用中的回调逻辑。
                 */
                () -> service.getMaintenanceRecord(saved.getRecordNo()));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    /**
     * 设备ResolutionIsScoped目标Its实验室AndExcludesSoftDeleted设备的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void deviceResolutionIsScopedToItsLabAndExcludesSoftDeletedDevices() throws Exception {
        String mapperXml = mapperXml();

        assertTrue(mapperXml.contains("d.lab_id = #{labId}"));
        assertTrue(mapperXml.contains("coalesce(d.deleted, 0) = 0"));
        assertEquals(
                Long.class,
                MaintenanceRecordMapper.class
                        .getDeclaredMethod("findDeviceId", String.class, Long.class)
                        .getReturnType()
        );
    }

    /**
     * mapped端点的处理逻辑。
     * @param controller controller参数。
     * @return 当前操作得到的记录集合。
     */
    private Set<String> mappedEndpoints(Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .flatMap(/** 转换并展开当前条目。 入参：method。 */ method -> mappings(method).stream())
                .collect(Collectors.toSet());
    }

    /**
     * mappings的处理逻辑。
     * @param method 方法参数。
     * @return 当前操作得到的记录集合。
     */
    private List<String> mappings(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            return paths("GET", method.getAnnotation(GetMapping.class).value());
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            return paths("POST", method.getAnnotation(PostMapping.class).value());
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            return paths("PUT", method.getAnnotation(PutMapping.class).value());
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            return paths("DELETE", method.getAnnotation(DeleteMapping.class).value());
        }
        return List.of();
    }

    /**
     * 路径的处理逻辑。
     * @param method 方法参数。
     * @param values 值集合参数。
     * @return 当前操作得到的记录集合。
     */
    private List<String> paths(String method, String[] values) {
        return Arrays.stream(values).map(/** 将当前条目转换为目标结果。 入参：value。 */ value -> method + " " + value).toList();
    }

    /**
     * 数据映射器Xml的处理逻辑。
     * @return String 类型的处理结果。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    private String mapperXml() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mapper/MaintenanceRecordMapper.xml")) {
            assertTrue(input != null, "MaintenanceRecordMapper.xml should be on the test classpath");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 有效记录的处理逻辑。
     * @return MaintenanceRecord 类型的处理结果。
     */
    private MaintenanceRecord validRecord() {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setLab("LAB-01");
        record.setDevice("DEV-01");
        record.setMaintenanceType("巡检");
        record.setMaintenanceDate("2026-10-10 10:00:00");
        record.setMaintainer("维护员");
        record.setContent("完成设备巡检");
        record.setResult("正常");
        record.setRemark("例行维护");
        return record;
    }

    private static class RecordingMapper implements MaintenanceRecordMapper {
        private Long labId = 11L;
        private Long deviceId = 12L;
        private Long insertedLabId;
        private Long insertedDeviceId;
        private Long requestedDeviceLabId;
        private MaintenanceRecord saved;
        private boolean deleteCalled;

        /**
         * 列出维护记录集合。
         * @return 当前操作得到的记录集合。
         */
        @Override
        public List<MaintenanceRecord> listMaintenanceRecords() {
            return saved == null ? List.of() : List.of(saved);
        }

        /**
         * 获取维护记录。
         * @param id 标识参数。
         * @return MaintenanceRecord 类型的处理结果。
         */
        @Override
        public MaintenanceRecord getMaintenanceRecord(String id) {
            return saved != null && (id.equals(saved.getRecordNo()) || id.equals(saved.getId())) ? saved : null;
        }

        /**
         * 查找实验室标识。
         * @param reference 引用参数。
         * @return Long 类型的处理结果。
         */
        @Override
        public Long findLabId(String reference) {
            return labId;
        }

        /**
         * 查找设备标识。
         * @param reference 引用参数。
         * @param labId 实验室标识参数。
         * @return Long 类型的处理结果。
         */
        @Override
        public Long findDeviceId(String reference, Long labId) {
            requestedDeviceLabId = labId;
            return deviceId;
        }

        /**
         * 插入维护记录。
         * @param record 记录参数。
         * @param labId 实验室标识参数。
         * @param deviceId 设备标识参数。
         * @return int 类型的处理结果。
         */
        @Override
        public int insertMaintenanceRecord(MaintenanceRecord record, Long labId, Long deviceId) {
            insertedLabId = labId;
            insertedDeviceId = deviceId;
            record.setId("99");
            saved = record;
            return 1;
        }

        /**
         * 更新维护记录。
         * @param id 标识参数。
         * @param record 记录参数。
         * @param labId 实验室标识参数。
         * @param deviceId 设备标识参数。
         * @return int 类型的处理结果。
         */
        @Override
        public int updateMaintenanceRecord(String id, MaintenanceRecord record, Long labId, Long deviceId) {
            insertedLabId = labId;
            insertedDeviceId = deviceId;
            record.setId(id);
            saved = record;
            return 1;
        }

        /**
         * 删除维护记录。
         * @param id 标识参数。
         * @return int 类型的处理结果。
         */
        @Override
        public int deleteMaintenanceRecord(String id) {
            deleteCalled = true;
            saved = null;
            return 1;
        }
    }
}
