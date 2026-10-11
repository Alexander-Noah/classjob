package org.example.backend.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityAccessPolicyTest {
    private final Object policy = newPolicy();

    @Test
    void userAdministrationRequiresSystemAdmin() {
        assertTrue(isAllowed("GET", "/users", "systemAdmin"));
        assertTrue(isAllowed("POST", "/admin/users/1/reset-password", "systemAdmin"));
        assertTrue(isAllowed("GET", "/users/options", "labAdmin"));

        assertFalse(isAllowed("GET", "/users", "teacher"));
        assertFalse(isAllowed("GET", "/users/options", "teacher"));
        assertFalse(isAllowed("POST", "/admin/users/1/reset-password", "labAdmin"));
        assertFalse(isAllowed("POST", "/users/options", "labAdmin"));
        assertFalse(isAllowed("POST", "/users/batch/role", "maintenance"));
    }

    @Test
    void labAdminCanManageLabResourcesButTeacherCannotMutateThem() {
        assertTrue(isAllowed("POST", "/devices/import", "labAdmin"));
        assertTrue(isAllowed("PUT", "/labs/12", "labAdmin"));
        assertTrue(isAllowed("POST", "/class-timetables/crawl", "labAdmin"));
        assertTrue(isAllowed("POST", "/reservations/9/approve", "labAdmin"));
        assertTrue(isAllowed("POST", "/repairs/9/assign", "labAdmin"));
        assertTrue(isAllowed("POST", "/notices", "labAdmin"));
        assertTrue(isAllowed("POST", "/usage-records/9/review", "labAdmin"));
        assertTrue(isAllowed("GET", "/business-loop/overview", "labAdmin"));

        assertFalse(isAllowed("POST", "/devices/import", "teacher"));
        assertFalse(isAllowed("PUT", "/labs/12", "teacher"));
        assertFalse(isAllowed("POST", "/reservations/9/approve", "teacher"));
    }

    @Test
    void teacherKeepsItsExpectedSurfaceAndRetiredMaintenanceRoleHasNone() {
        assertTrue(isAllowed("GET", "/class-timetables", "teacher"));
        assertTrue(isAllowed("GET", "/reservations", "teacher"));
        assertTrue(isAllowed("POST", "/reservations", "teacher"));
        assertFalse(isAllowed("POST", "/class-timetables/crawl", "teacher"));

        assertFalse(isAllowed("GET", "/repairs", "maintenance"));
        assertFalse(isAllowed("POST", "/repairs/RO-2026/complete", "maintenance"));
        assertFalse(isAllowed("GET", "/dashboard", "maintenance"));
        assertFalse(isAllowed("DELETE", "/users/1", "maintenance"));
    }

    private Object newPolicy() {
        try {
            return Class.forName("org.example.backend.security.SecurityAccessPolicy")
                    .getConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private boolean isAllowed(String method, String path, String roleCode) {
        try {
            return (Boolean) policy.getClass()
                    .getMethod("isAllowed", String.class, String.class, String.class)
                    .invoke(policy, method, path, roleCode);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
