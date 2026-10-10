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
        String mapper = Files.readString(Path.of("src/main/resources/mapper/DeviceEventMapper.xml"));
        String service = Files.readString(Path.of("src/main/java/org/example/backend/service/impl/DevicesServiceImpl.java"));
        assertAll(
                () -> assertTrue(mapper.contains("insert into device_event")),
                () -> assertTrue(mapper.contains("event_type = 'inventory'")),
                () -> assertTrue(mapper.contains("event_type = 'transfer'")),
                () -> assertFalse(service.contains("createTableIfNotExists")),
                () -> assertFalse(service.contains("DeviceInventoryRecordMapper")),
                () -> assertFalse(service.contains("DeviceTransferRecordMapper")),
                () -> assertTrue(service.contains("DeviceEventMapper"))
        );
    }
}
