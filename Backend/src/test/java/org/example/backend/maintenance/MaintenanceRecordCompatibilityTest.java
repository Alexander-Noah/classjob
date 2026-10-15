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

    @Test
    void legacyRowsUseNotProvidedForMissingMaintenanceRecordFields() throws IOException {
        String mapperXml = mapperXml();

        for (String column : List.of(
                "recordNo", "lab", "device", "maintenanceType", "maintenanceDate",
                "maintainer", "content", "result", "remark", "updatedAt"
        )) {
            assertTrue(mapperXml.matches("(?s).*coalesce\\(.*'未填写'\\)\\s+as\\s+" + column + ".*"), column);
        }
    }

    @Test
    void maintenanceRecordProjectionOmitsLegacyWorkflowState() throws IOException {
        String mapperXml = mapperXml();
        Class<?> maintenanceRecord = org.example.backend.entity.MaintenanceRecord.class;

        assertFalse(Arrays.stream(maintenanceRecord.getDeclaredFields())
                .map(field -> field.getName())
                .anyMatch(field -> field.equals("status") || field.equals("progress")));
        assertFalse(mapperXml.substring(0, mapperXml.indexOf("</sql>")).contains("r.status"));
        assertFalse(mapperXml.substring(0, mapperXml.indexOf("</sql>")).contains("r.progress"));
    }

    @Test
    void legacyRepairsRemainReadable() {
        assertTrue(mappedEndpoints(RepairController.class).contains("GET /repairs"));
    }

    @Test
    void legacyRepairMutationsReturnGone() {
        RepairController controller = new RepairController(null);

        assertGone(() -> controller.createRepair(new RepairEntity()));
        assertGone(() -> controller.updateRepair("RO-001", new RepairEntity()));
        assertGone(() -> controller.deleteRepair("RO-001"));
        assertGone(() -> controller.replaceRepairs(new RepairController.RepairBatchRequest()));
        assertGone(controller::resetRepairs);
        assertGone(() -> controller.assignRepair("RO-001", new RepairEntity()));
        assertGone(() -> controller.submitAcceptance("RO-001", new RepairEntity()));
        assertGone(() -> controller.completeRepair("RO-001", new RepairEntity()));
    }

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

    private void assertGone(ThrowingRunnable operation) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, operation::run);
        assertEquals(HttpStatus.GONE, error.getStatusCode());
        assertEquals(RETIRED_WORKFLOW_MESSAGE, error.getReason());
    }

    private Set<String> mappedEndpoints(Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .flatMap(method -> mappings(method).stream())
                .collect(Collectors.toSet());
    }

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

    private List<String> paths(String method, String[] values) {
        return Arrays.stream(values).map(value -> method + " " + value).toList();
    }

    private String mapperXml() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mapper/MaintenanceRecordMapper.xml")) {
            assertTrue(input != null, "MaintenanceRecordMapper.xml should be on the test classpath");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
