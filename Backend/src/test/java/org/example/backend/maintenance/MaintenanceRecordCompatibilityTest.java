/*
 * 维护记录回归测试；测试夹具与生产配置分离。
 * 覆盖场景：legacyRowsUseNotProvidedForMissingMaintenanceRecordFields、maintenanceRecordProjectionOmitsLegacyWorkflowState、legacyRepairsRemainReadable、legacyRepairMutationsReturnGone 等。
 */
package org.example.backend.maintenance;

import org.example.backend.controller.RepairController;
import org.example.backend.entity.RepairEntity;
import org.example.backend.security.SecurityAccessPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MaintenanceRecordCompatibilityTest {
    private static final String RETIRED_WORKFLOW_MESSAGE = "故障工单流程已停用，请使用维护记录功能";

    /**
     * legacy行集合UseNotProvidedForMissing维护记录Fields的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void legacyRowsUseNotProvidedForMissingMaintenanceRecordFields() throws IOException {
        String mapperXml = mapperXml();

        for (String column : List.of(
                "recordNo", "lab", "maintenanceType", "maintenanceDate",
                "maintainer", "content", "result", "remark", "updatedAt"
        )) {
            assertTrue(mapperXml.matches("(?s).*coalesce\\(.*'未填写'\\)\\s+as\\s+" + column + ".*"), column);
        }
        assertTrue(mapperXml.contains("coalesce(d.device_name, '未关联设备') as device"));
    }

    /**
     * 维护记录ProjectionOmitsLegacyWorkflow状态的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void maintenanceRecordProjectionOmitsLegacyWorkflowState() throws IOException {
        String mapperXml = mapperXml();
        Class<?> maintenanceRecord = org.example.backend.entity.MaintenanceRecord.class;

        assertFalse(Arrays.stream(maintenanceRecord.getDeclaredFields())
                .map(/** 将当前条目转换为目标结果。 入参：field。 */ field -> field.getName())
                .anyMatch(/** 处理 anyMatch 调用中的回调逻辑。 入参：field。 */ field -> field.equals("status") || field.equals("progress")));
        assertFalse(mapperXml.substring(0, mapperXml.indexOf("</sql>")).contains("r.status"));
        assertFalse(mapperXml.substring(0, mapperXml.indexOf("</sql>")).contains("r.progress"));
    }

    /**
     * legacyRepairsRemainReadable的处理逻辑。
     */
    @Test
    void legacyRepairsRemainReadable() {
        assertTrue(mappedEndpoints(RepairController.class).contains("GET /repairs"));
    }

    /**
     * legacyRepairMutationsReturnGone的处理逻辑。
     */
    @Test
    void legacyRepairMutationsReturnGone() {
        RepairController controller = new RepairController(null);

        assertGone(/** 处理 assertGone 调用中的回调逻辑。 */ () -> controller.createRepair(new RepairEntity()));
        assertGone(/** 处理 assertGone 调用中的回调逻辑。 */ () -> controller.updateRepair("RO-001", new RepairEntity()));
        assertGone(/** 处理 assertGone 调用中的回调逻辑。 */ () -> controller.deleteRepair("RO-001"));
        assertGone(/** 处理 assertGone 调用中的回调逻辑。 */ () -> controller.replaceRepairs(new RepairController.RepairBatchRequest()));
        assertGone(controller::resetRepairs);
        assertGone(/** 处理 assertGone 调用中的回调逻辑。 */ () -> controller.assignRepair("RO-001", new RepairEntity()));
        assertGone(/** 处理 assertGone 调用中的回调逻辑。 */ () -> controller.submitAcceptance("RO-001", new RepairEntity()));
        assertGone(/** 处理 assertGone 调用中的回调逻辑。 */ () -> controller.completeRepair("RO-001", new RepairEntity()));
    }

    /**
     * only实验室AdminsCanAccess维护记录集合的处理逻辑。
     */
    @Test
    void onlyLabAdminsCanAccessMaintenanceRecords() {
        SecurityAccessPolicy policy = new SecurityAccessPolicy();

        assertTrue(policy.isAllowed("GET", "/maintenance-records", "labAdmin"));
        assertTrue(policy.isAllowed("GET", "/maintenance-records/MR-001", "labAdmin"));
        for (String role : List.of("teacher", "academic", "maintenance")) {
            assertFalse(policy.isAllowed("GET", "/maintenance-records", role), role);
            assertFalse(policy.isAllowed("GET", "/maintenance-records/MR-001", role), role);
        }
        for (String method : List.of("POST", "PUT", "DELETE")) {
            assertTrue(policy.isAllowed(method, "/maintenance-records/MR-001", "labAdmin"), method);
            assertFalse(policy.isAllowed(method, "/maintenance-records/MR-001", "teacher"), method);
            assertFalse(policy.isAllowed(method, "/maintenance-records/MR-001", "academic"), method);
            assertFalse(policy.isAllowed(method, "/maintenance-records/MR-001", "maintenance"), method);
        }
    }

    /**
     * 断言Gone。
     * @param operation 操作参数。
     */
    private void assertGone(ThrowingRunnable operation) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, operation::run);
        assertEquals(HttpStatus.GONE, error.getStatusCode());
        assertEquals(RETIRED_WORKFLOW_MESSAGE, error.getReason());
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

    @FunctionalInterface
    private interface ThrowingRunnable {
        /**
         * 运行当前操作。
         * @throws Exception 当前调用无法继续完成时抛出。
         */
        void run() throws Exception;
    }
}
