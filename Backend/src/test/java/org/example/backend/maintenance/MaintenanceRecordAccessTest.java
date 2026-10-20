/*
 * 维护记录回归测试；测试夹具与生产配置分离。
 * 覆盖场景：legacyWritePermissionsWorkButReadOnlyPermissionsDoNot。
 */
package org.example.backend.maintenance;

import org.example.backend.security.MaintenanceRecordAccess;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaintenanceRecordAccessTest {
    @Test
    void optionalLegacyPermissionsAndExplicitReadOnlyGroupsRemainDistinct() {
        for (String permissions : new String[]{null, "", "  ", "[]"})
            assertTrue(MaintenanceRecordAccess.canWrite("labAdmin", permissions));
        for (String permissions : new String[]{"null", "{}", "true", "[1,null,{}, {\"items\":null}, {\"items\":\"故障报修\"}]", "[\"维护记录查看\"]"})
            assertFalse(MaintenanceRecordAccess.canWrite("labAdmin", permissions), String.valueOf(permissions));
        assertTrue(MaintenanceRecordAccess.canWrite("labAdmin", "[{}, {\"items\":[\"维护记录查看\",\"故障报修\"]}]"));
        assertTrue(MaintenanceRecordAccess.canWrite("systemAdmin", "broken-json"));
        assertFalse(MaintenanceRecordAccess.canWrite(null, null));
        assertFalse(MaintenanceRecordAccess.canWrite("academic", "[\"故障报修\"]"));
    }
    /**
     * legacyWrite权限WorkButReadOnly权限DoNot的处理逻辑。
     */
    @Test
    void legacyWritePermissionsWorkButReadOnlyPermissionsDoNot() {
        assertTrue(MaintenanceRecordAccess.canWrite("labAdmin", "[{\"items\":[\"维护记录填写\",\"维护记录查看\"]}]"));
        assertTrue(MaintenanceRecordAccess.canWrite("labAdmin", "[\"维护记录管理\"]"));
        assertFalse(MaintenanceRecordAccess.canWrite("labAdmin", "[{\"items\":[\"维护记录查看\"]}]"));
        assertFalse(MaintenanceRecordAccess.canWrite("labAdmin", "invalid-json"));
        assertFalse(MaintenanceRecordAccess.canWrite("teacher", "[\"维护记录填写\"]"));
    }
}
