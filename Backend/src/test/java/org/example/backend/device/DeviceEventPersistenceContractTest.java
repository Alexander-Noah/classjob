package org.example.backend.device;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceEventPersistenceContractTest {
    @Test
    void deviceLifecycleUsesOneEventTableWithoutRuntimeDdl() throws Exception {
        String mapper = Files.readString(resolveModulePath("src", "main", "resources", "mapper", "DeviceEventMapper.xml"));
        String service = Files.readString(resolveModulePath("src", "main", "java", "org", "example", "backend", "service", "impl", "DevicesServiceImpl.java"));
        String inventoryInsert = between(mapper, "<insert id=\"insertInventoryEvent\"", "</insert>");
        String transferInsert = between(mapper, "<insert id=\"insertTransferEvent\"", "</insert>");
        assertAll(
                () -> assertTrue(mapper.contains("insert into device_event")),
                () -> assertTrue(mapper.contains("event_type = 'inventory'")),
                () -> assertTrue(mapper.contains("event_type = 'transfer'")),
                () -> assertTrue(inventoryInsert.contains("useGeneratedKeys=\"true\" keyProperty=\"id\"")),
                () -> assertTrue(transferInsert.contains("useGeneratedKeys=\"true\" keyProperty=\"id\"")),
                () -> assertFalse(service.contains("createTableIfNotExists")),
                () -> assertFalse(service.contains("DeviceInventoryRecordMapper")),
                () -> assertFalse(service.contains("DeviceTransferRecordMapper")),
                () -> assertTrue(service.contains("DeviceEventMapper"))
        );
    }

    private Path resolveModulePath(String... parts) {
        Path modulePath = Path.of(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            modulePath = modulePath.resolve(parts[i]);
        }
        return modulePath;
    }

    private String between(String value, String startMarker, String endMarker) {
        int start = value.indexOf(startMarker);
        assertTrue(start >= 0, startMarker + " should exist");
        int end = value.indexOf(endMarker, start);
        assertTrue(end > start, endMarker + " should exist after " + startMarker);
        return value.substring(start, end + endMarker.length());
    }
}
