package org.example.backend.service;

import org.example.backend.service.impl.IotDeviceServiceImpl;
import org.example.backend.service.gateway.IotGatewayCommandService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mockito.invocation.Invocation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IotDeviceServiceBehaviorTest {
    private List<Map<String, Object>> rows = new ArrayList<>();
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class, invocation -> {
        if (invocation.getMethod().getName().equals("queryForList")) return rows;
        if (invocation.getMethod().getName().equals("update")) return 1;
        return RETURNS_DEFAULTS.answer(invocation);
    });
    private final IotGatewayCommandService gateway = mock(IotGatewayCommandService.class);
    private final IotDeviceServiceImpl service = new IotDeviceServiceImpl(jdbc, gateway);

    @Test
    void deviceFiltersAreBoundAsParametersAndBlankFiltersAreOmitted() {
        rows = List.of(Map.of("id", 1));
        String hostile = "x' OR 1=1 --";
        assertEquals(Map.of("total", 1, "devices", rows), service.listDevices(Map.of(
                "labId", " 7 ", "deviceType", "camera", "status", "online", "keyword", hostile)));
        Invocation query = last("queryForList");
        String sql = query.getArgument(0);
        assertTrue(sql.contains("coalesce(deleted, 0) = 0"));
        assertTrue(sql.contains("lab_id = ?"));
        assertFalse(sql.contains(hostile));
        assertEquals(List.of("7", "camera", "online", "%" + hostile + "%", "%" + hostile + "%", "%" + hostile + "%"), parameters(query));
        service.listDevices(Map.of("keyword", " ", "status", ""));
        assertEquals(List.of(), parameters(last("queryForList")));
    }

    @Test
    void labListsCountAllConfiguredRepresentationsAndReturnEnvironmentAndOverview() {
        rows = List.of(Map.of("configured", true), Map.of("configured", 1), Map.of("configured", "yes"), Map.of("configured", "true"), Map.of("configured", "1"), Map.of("configured", 0), Map.of());
        var result = service.listLabDevices(7L);
        assertEquals(5L, result.get("configuredCount"));
        assertEquals(7, result.get("total"));
        assertEquals(7L, result.get("labId"));
        assertEquals(List.of(7L), parameters(last("queryForList")));
        assertEquals(Map.of("labId", 7L, "types", rows), service.getLabOverview(7L));
        assertEquals(Map.of("labId", 7L, "total", 7, "metrics", rows), service.getEnvironment(7L));
    }

    @Test
    void createRequiresLabAndNormalizesDeviceDefaultsBeforeReadingSavedRecord() {
        assertThrows(IllegalArgumentException.class, () -> service.createDevice(null));
        assertThrows(IllegalArgumentException.class, () -> service.createDevice(Map.of("labId", "bad")));
        verifyNoInteractions(jdbc);
        rows = List.of(Map.of("id", 8, "iotCode", "CAM-1"));
        assertEquals(rows.get(0), service.createDevice(Map.of("labId", "7", "code", " CAM-1 ", "deviceType", "IP-CAMERA", "configured", "yes", "enabled", false)));
        List<Object> inserted = parameters(last("update"));
        assertEquals(List.of("CAM-1", "CAM-1", "ip_camera", 7L), inserted.subList(0, 4));
        assertEquals("mock", inserted.get(7));
        assertEquals(true, inserted.get(11));
        assertEquals(false, inserted.get(12));
        assertEquals("unconfigured", inserted.get(13));
        assertEquals("manual", inserted.get(16));
        assertEquals(List.of("CAM-1"), parameters(last("queryForList")));
        service.createDevice(Map.of("labId", 7));
        inserted = parameters(last("update"));
        assertTrue(inserted.get(0).toString().startsWith("IOT-"));
        assertEquals("access_control", inserted.get(2));
        assertEquals(true, inserted.get(12));
    }

    @Test
    void updateBindsIdentifiersAndDeleteIsLogical() {
        rows = List.of(Map.of("id", 8));
        assertEquals(rows.get(0), service.updateDevice(8L, Map.of("name", "门禁", "labId", 7, "assetDeviceId", "4", "configured", 1)));
        List<Object> updated = parameters(last("update"));
        assertEquals("门禁", updated.get(0));
        assertEquals(7L, updated.get(2));
        assertEquals(4L, updated.get(5));
        assertEquals(8L, updated.get(17));
        assertEquals(List.of(8L), parameters(last("queryForList")));
        service.updateDevice(8L, null);
        assertNull(parameters(last("update")).get(2));
        service.deleteDevice(8L);
        assertTrue(last("update").getArgument(0).toString().contains("set deleted = 1"));
        assertEquals(List.of(8L), parameters(last("update")));
    }

    @Test
    void controlResolvesIdBeforeCodeAndDelegatesOnlyExistingDevices() {
        assertThrows(IllegalArgumentException.class, () -> service.control(null));
        rows = List.of(Map.of("id", 8));
        when(gateway.execute(anyMap(), anyMap())).thenReturn(Map.of("result", "success"));
        for (Map<String, Object> request : List.of(Map.<String, Object>of("iotDeviceId", 8, "code", "ignored"), Map.<String, Object>of("id", "8"))) {
            assertEquals(Map.of("result", "success"), service.control(request));
            assertEquals(List.of(8L), parameters(last("queryForList")));
            verify(gateway).execute(rows.get(0), request);
        }
        service.control(Map.of("iotDeviceId", "bad", "iotCode", "CAM-1"));
        assertEquals(List.of("CAM-1"), parameters(last("queryForList")));
        rows = List.of();
        assertThrows(IllegalArgumentException.class, () -> service.control(Map.of("id", 999)));
        assertThrows(IllegalArgumentException.class, () -> service.control(Map.of("code", "missing")));
        verify(gateway, times(3)).execute(anyMap(), anyMap());
    }

    @Test
    void logsBindFiltersAndDefaultMalformedLimits() {
        assertEquals(Map.of("total", 0, "records", List.of()), service.listCommandLogs(Map.of()));
        assertEquals(List.of(100), parameters(last("queryForList")));
        service.listCommandLogs(Map.of("labId", "7", "iotDeviceId", "8", "resultStatus", "success", "limit", "25"));
        assertEquals(List.of("7", "8", "success", 25), parameters(last("queryForList")));
        service.listCommandLogs(Map.of("limit", "invalid"));
        assertEquals(List.of(100), parameters(last("queryForList")));
    }

    private Invocation last(String method) {
        return mockingDetails(jdbc).getInvocations().stream().filter(call -> call.getMethod().getName().equals(method)).reduce((a, b) -> b).orElseThrow();
    }
    private List<Object> parameters(Invocation invocation) {
        Object[] arguments = invocation.getArguments();
        return Arrays.asList(Arrays.copyOfRange(arguments, 1, arguments.length));
    }
}
