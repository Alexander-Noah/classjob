package org.example.backend.database;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

class RetiredModuleBoundaryTest {
    private static final Set<String> RETIRED_ROUTE_MARKERS = Set.of(
            "/ai-assistant", "/activation-codes", "/teacher-hosts", "/student-clients", "/iot"
    );

    @Test
    void productionSourcesReferenceOnlyTargetBusinessTables() throws Exception {
        Set<String> retired = Set.of(
                "ai_assistant_conversation", "ai_assistant_user_config", "approval_countersign",
                "consumable_inventory", "consumable_stock_record", "course_environment_request",
                "device_inventory_record", "device_runtime_status", "device_status_event", "device_transfer_record",
                "environment_template", "host_static_asset", "iot_command_log", "iot_device",
                "iot_device_capability", "iot_gateway_config", "iot_gateway_point", "iot_telemetry_latest",
                "lab_activation_code", "lab_software", "lab_terminal", "module_record", "schedule_adjustment",
                "student_client", "teacher_host", "timetable_academic_credential",
                "timetable_crawler_config", "timetable_crawler_task"
        );
        String production = readProductionSources();
        for (String table : retired) {
            assertFalse(production.contains(table), table);
        }
        assertFalse(production.toLowerCase().contains("create table"));
        assertFalse(production.toLowerCase().contains("alter table"));
        assertFalse(production.toLowerCase().contains("drop table"));
        for (String route : RETIRED_ROUTE_MARKERS) {
            assertFalse(production.contains(route), route);
        }
        assertFalse(production.toLowerCase().contains("vosk"));
    }

    private String readProductionSources() throws IOException {
        Path root = Path.of("src", "main");
        StringBuilder content = new StringBuilder();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String normalized = path.toString().replace('\\', '/');
                if (normalized.contains("/resources/db/migration/")) {
                    continue;
                }
                content.append(Files.readString(path)).append('\n');
            }
        }
        return content.toString();
    }
}
