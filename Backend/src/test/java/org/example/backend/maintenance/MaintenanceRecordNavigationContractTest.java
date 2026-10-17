/*
 * 维护记录回归测试；测试夹具与生产配置分离。
 * 覆盖场景：backendDefaultsPointToMaintenanceRecordViewing、maintenanceRoleCannotAccessAnyAuthenticatedBusinessSurface、validMaintenanceCredentialsReturnTheRetiredRoleMessage、existingMaintenanceSessionIsRejectedWithTheRetiredRoleMessage。
 */
package org.example.backend.maintenance;

import jakarta.servlet.FilterChain;
import org.example.backend.entity.UserEntity;
import org.example.backend.mapper.UserMapper;
import org.example.backend.security.JwtAuthenticationFilter;
import org.example.backend.security.JwtService;
import org.example.backend.security.PasswordService;
import org.example.backend.security.SecurityAccessPolicy;
import org.example.backend.service.BusinessLoopService;
import org.example.backend.service.impl.DashboardServiceImpl;
import org.example.backend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaintenanceRecordNavigationContractTest {
    private static final String RETIRED_ROLE_MESSAGE = "角色已停用，请联系系统管理员重新分配角色";

    @Test
    void backendDefaultsPointToMaintenanceRecordViewing() throws Exception {
        DashboardServiceImpl dashboardService = new DashboardServiceImpl(null, null, null);
        Method buildShortcuts = DashboardServiceImpl.class.getDeclaredMethod("buildShortcuts");
        buildShortcuts.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> shortcuts = (List<Map<String, Object>>) buildShortcuts.invoke(dashboardService);
        Map<String, Object> maintenanceShortcut = shortcuts.stream()
                .filter(shortcut -> "维护记录查看".equals(shortcut.get("title")))
                .findFirst()
                .orElseThrow();

        assertEquals("/lab-admin/maintenance-records", maintenanceShortcut.get("path"));
        assertFalse(shortcuts.stream().anyMatch(shortcut -> "/lab-admin/repairs".equals(shortcut.get("path"))));

        UserServiceImpl userService = new UserServiceImpl(null, null, null);
        Method defaultPermissions = UserServiceImpl.class.getDeclaredMethod("defaultPermissions", String.class);
        defaultPermissions.setAccessible(true);
        String permissions = String.valueOf(defaultPermissions.invoke(userService, "labAdmin"));

        assertTrue(permissions.contains("维护记录填写"));
        assertTrue(permissions.contains("维护记录查看"));
        assertFalse(permissions.contains("故障报修"));
        assertFalse(permissions.contains("维修工单"));
    }

    @Test
    void maintenanceRoleCannotAccessAnyAuthenticatedBusinessSurface() {
        SecurityAccessPolicy policy = new SecurityAccessPolicy();

        for (String path : List.of("/repairs", "/devices", "/notices", "/maintenance-records", "/dashboard")) {
            assertFalse(policy.isAllowed("GET", path, "maintenance"), path);
        }
        assertTrue(policy.isAllowed("GET", "/maintenance-records", "labAdmin"));
        assertFalse(policy.isAllowed("GET", "/maintenance-records", "teacher"));
    }

    @Test
    void validMaintenanceCredentialsReturnTheRetiredRoleMessage() {
        UserMapper userMapper = mock(UserMapper.class);
        BusinessLoopService businessLoopService = mock(BusinessLoopService.class);
        PasswordService passwordService = mock(PasswordService.class);
        UserEntity legacyUser = new UserEntity();
        legacyUser.setId(42);
        legacyUser.setUsername("repair01");
        legacyUser.setPassword("encoded");
        legacyUser.setStatus(1);
        legacyUser.setRoleCode("maintenance");
        legacyUser.setRoleName("维修人员");

        when(userMapper.login("repair01")).thenReturn(legacyUser);
        when(passwordService.matches("Valid123", "encoded")).thenReturn(true);

        UserServiceImpl service = new UserServiceImpl(userMapper, businessLoopService, passwordService);
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.login("repair01", "Valid123")
        );

        assertEquals(RETIRED_ROLE_MESSAGE, error.getMessage());
        verify(userMapper, never()).updatePassword(42, "encoded");
    }

    @Test
    void existingMaintenanceSessionIsRejectedWithTheRetiredRoleMessage() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        when(jwtService.validateToken(anyString()))
                .thenReturn(new JwtService.JwtClaims("repair01", "maintenance", "42", Long.MAX_VALUE));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        request.addHeader("Authorization", "Bearer legacy-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains(RETIRED_ROLE_MESSAGE));
        verify(chain, never()).doFilter(request, response);
    }
}
