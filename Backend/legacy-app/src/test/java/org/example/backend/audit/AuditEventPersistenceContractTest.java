package org.example.backend.audit;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditEventPersistenceContractTest {
    @Test
    void businessLoopAppendsStructuredAuditEvents() throws Exception {
        String service = Files.readString(Path.of("src/main/java/org/example/backend/service/impl/BusinessLoopServiceImpl.java"));
        String xml = Files.readString(Path.of("src/main/resources/mapper/AuditEventMapper.xml"));
        assertAll(
                () -> assertTrue(service.contains("AuditEventMapper")),
                () -> assertFalse(service.contains("ModuleRecordMapper")),
                () -> assertTrue(xml.contains("insert into audit_event")),
                () -> assertFalse(xml.contains("delete from audit_event")),
                () -> assertFalse(xml.contains("module_record"))
        );
    }
}
