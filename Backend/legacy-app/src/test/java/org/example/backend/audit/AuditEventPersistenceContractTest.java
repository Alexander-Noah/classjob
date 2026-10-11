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
        String mapper = Files.readString(Path.of("src/main/java/org/example/backend/mapper/AuditEventMapper.java"));
        String xml = Files.readString(Path.of("src/main/resources/mapper/AuditEventMapper.xml"));
        String report = Files.readString(Path.of("src/main/resources/mapper/BusinessLoopReportMapper.xml"));
        assertAll(
                () -> assertTrue(service.contains("AuditEventMapper")),
                () -> assertFalse(service.contains("ModuleRecordMapper")),
                () -> assertTrue(service.contains("redact") || service.contains("sanitize")),
                () -> assertTrue(mapper.contains("countByTypeAndRange")),
                () -> assertTrue(xml.contains("insert into audit_event")),
                () -> assertFalse(xml.contains("delete from audit_event")),
                () -> assertFalse(xml.contains("module_record")),
                () -> assertTrue(report.contains("from audit_event")),
                () -> assertFalse(report.contains("module_record"))
        );
    }

    @Test
    void auditPersistenceIsAppendOnlyAndUsesStructuredColumns() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/mapper/AuditEventMapper.xml"));
        assertAll(
                () -> assertTrue(xml.contains("aggregate_type")),
                () -> assertTrue(xml.contains("aggregate_id")),
                () -> assertTrue(xml.contains("detail_json")),
                () -> assertTrue(xml.contains("occurred_at")),
                () -> assertTrue(xml.contains("order by occurred_at desc, id desc"))
        );
    }
}
