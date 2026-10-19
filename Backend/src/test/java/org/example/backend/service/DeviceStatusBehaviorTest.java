package org.example.backend.service;

import org.example.backend.service.impl.DeviceStatusServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeviceStatusBehaviorTest {
    private List<Map<String, Object>> terminal = List.of();
    private List<Map<String, Object>> runtime = List.of();
    private List<Map<String, Object>> generic = List.of();
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class, call -> {
        if (call.getMethod().getName().equals("queryForList")) {
            String sql = call.getArgument(0);
            if (sql.contains("from lab_terminal")) return terminal;
            if (sql.startsWith("select id, online_status")) return runtime;
            return generic;
        }
        if (call.getMethod().getName().equals("update")) return 1;
        return RETURNS_DEFAULTS.answer(call);
    });
    private final DeviceStatusServiceImpl service = new DeviceStatusServiceImpl(jdbc);

    @Test
    void runtimeFiltersAndEventQueriesBindAllUserValues() {
        generic = List.of(Map.of("id", 1));
        var result = service.listLabRuntimeStatus(7L, Map.of("onlineStatus", "online", "runtimeStatus", "normal", "health", "good", "sourceType", "terminal", "keyword", "host' --"));
        assertEquals(1, result.get("total"));
        var query = lastQuery();
        assertFalse(query.getArgument(0).toString().contains("host' --"));
        assertEquals(List.of(7L, "online", "normal", "good", "terminal", "%host' --%", "%host' --%", "%host' --%", "%host' --%"), args(query));
        assertEquals(generic.get(0), service.getDeviceRuntimeStatus(1L));
        assertEquals(List.of(1L), args(lastQuery()));
        generic = List.of(); assertEquals(Map.of(), service.getDeviceRuntimeStatus(99L));
        service.listEvents(Map.of("labId", "7", "deviceId", "1", "terminalId", "host", "eventType", "manual", "eventLevel", "warning", "limit", "25"));
        assertEquals(List.of("7", "1", "host", "manual", "warning", 25), args(lastQuery()));
        service.listEvents(Map.of("limit", "bad")); assertEquals(List.of(100), args(lastQuery()));
    }

    @Test
    void summaryHasZeroDefaultsAndPassesThroughServerAggregates() {
        assertEquals(Map.of("total", 0, "online", 0, "offline", 0, "abnormal", 0), service.getSummary(Map.of()));
        generic = List.of(Map.of("total", 10, "online", 8));
        assertEquals(generic.get(0), service.getSummary(Map.of("labId", "7", "sourceType", "terminal")));
        assertEquals(List.of("7", "terminal"), args(lastQuery()));
    }

    @Test
    void newReportUsesDefaultsAndWritesBothRuntimeSnapshotAndOnlineEvent() {
        assertThrows(IllegalArgumentException.class, () -> service.reportRuntimeStatus(null));
        var result = service.reportRuntimeStatus(Map.of("labId", "7", "deviceId", "4", "cpuUsage", "12.5", "memoryUsage", 50, "diskUsage", new BigDecimal("80.5")));
        assertEquals(true, result.get("accepted")); assertEquals(7L, result.get("labId"));
        assertEquals("online", result.get("onlineStatus")); assertEquals("normal", result.get("runtimeStatus")); assertEquals("unknown", result.get("health"));
        var snapshot = args(updateContaining("insert into device_runtime_status"));
        assertEquals(7L, snapshot.get(0)); assertEquals(4L, snapshot.get(3));
        assertEquals(new BigDecimal("12.5"), snapshot.get(13)); assertEquals(new BigDecimal("50.0"), snapshot.get(14)); assertEquals(new BigDecimal("80.5"), snapshot.get(15));
        var event = args(updateContaining("insert into device_status_event"));
        assertEquals("online", event.get(5)); assertEquals("info", event.get(6)); assertEquals("new", event.get(7)); assertEquals("normal/online", event.get(8));
    }

    @Test
    void terminalCredentialsAreHashedAndCanonicalLabTakesPrecedenceOverSubmittedId() {
        terminal = List.of(Map.of("id", 11L, "terminalId", "host-1", "labId", 7L, "labCode", "A101", "labName", "机房"));
        runtime = List.of(Map.of("id", 20L, "onlineStatus", "online", "runtimeStatus", "normal"));
        var result = service.reportRuntimeStatus(Map.of("terminalId", "host-1", "terminalToken", " test-only-token ", "labId", 999));
        assertEquals(7L, result.get("labId")); assertEquals("host-1", result.get("terminalId"));
        var credentialQuery = mockingDetails(jdbc).getInvocations().stream().filter(call -> call.getMethod().getName().equals("queryForList") && call.getArgument(0).toString().contains("from lab_terminal")).findFirst().orElseThrow();
        var bound = args(credentialQuery);
        assertEquals("host-1", bound.get(0)); assertEquals(64, bound.get(1).toString().length()); assertNotEquals("test-only-token", bound.get(1));
        var snapshot = args(updateContaining("insert into device_runtime_status"));
        assertEquals("A101", snapshot.get(1)); assertEquals("机房", snapshot.get(2)); assertEquals(11L, snapshot.get(4));
        assertEquals("heartbeat", args(updateContaining("insert into device_status_event")).get(5));
        assertEquals(11L, args(updateContaining("update lab_terminal")).get(2));
    }

    @Test
    void changedRuntimeWritesDangerEventAndInvalidMetricsBecomeNull() {
        runtime = List.of(Map.of("id", 20, "onlineStatus", "online", "runtimeStatus", "normal"));
        service.reportRuntimeStatus(Map.of("labId", 7, "deviceId", 4, "onlineStatus", "offline", "runtimeStatus", "abnormal", "cpuUsage", "bad", "memoryUsage", "", "diskUsage", "invalid"));
        var event = args(updateContaining("insert into device_status_event"));
        assertEquals("manual", event.get(5)); assertEquals("danger", event.get(6)); assertEquals("normal", event.get(7));
        var snapshot = args(updateContaining("insert into device_runtime_status"));
        assertNull(snapshot.get(13)); assertNull(snapshot.get(14)); assertNull(snapshot.get(15));
    }

    @Test
    void missingTerminalAndDeviceIdentifiersStillPermitExplicitLabReportsButNoLabRejects() {
        assertThrows(IllegalArgumentException.class, () -> service.reportRuntimeStatus(Map.of("terminalToken", "unknown")));
        service.reportRuntimeStatus(Map.of("labId", 7));
        assertEquals("online", args(updateContaining("insert into device_status_event")).get(5));
        assertThrows(IllegalArgumentException.class, () -> service.reportRuntimeStatus(Map.of("labId", "not-a-number")));
    }

    private Invocation lastQuery() { return mockingDetails(jdbc).getInvocations().stream().filter(call -> call.getMethod().getName().equals("queryForList")).reduce((a,b) -> b).orElseThrow(); }
    private Invocation updateContaining(String sql) { return mockingDetails(jdbc).getInvocations().stream().filter(call -> call.getMethod().getName().equals("update") && call.getArgument(0).toString().contains(sql)).reduce((a,b) -> b).orElseThrow(); }
    private List<Object> args(Invocation call) { Object[] values = call.getArguments(); return Arrays.asList(Arrays.copyOfRange(values, 1, values.length)); }
}
