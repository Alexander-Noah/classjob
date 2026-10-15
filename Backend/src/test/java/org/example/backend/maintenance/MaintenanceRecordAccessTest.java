package org.example.backend.maintenance;

import org.example.backend.security.MaintenanceRecordAccess;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaintenanceRecordAccessTest {
    @Test
    void legacyWritePermissionsWorkButReadOnlyPermissionsDoNot() {
        assertTrue(MaintenanceRecordAccess.canWrite("labAdmin", "[{\"items\":[\"维护记录填写\",\"维护记录查看\"]}]"));
        assertTrue(MaintenanceRecordAccess.canWrite("labAdmin", "[\"维护记录管理\"]"));
        assertFalse(MaintenanceRecordAccess.canWrite("labAdmin", "[{\"items\":[\"维护记录查看\"]}]"));
        assertFalse(MaintenanceRecordAccess.canWrite("labAdmin", "invalid-json"));
        assertFalse(MaintenanceRecordAccess.canWrite("teacher", "[\"维护记录填写\"]"));
    }
}
