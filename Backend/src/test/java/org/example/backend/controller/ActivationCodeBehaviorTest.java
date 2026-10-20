package org.example.backend.controller;

import org.example.backend.result.Result;
import org.example.backend.service.BusinessLoopService;
import org.example.backend.service.ModuleRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ActivationCodeBehaviorTest {
    private final Map<String, List<Map<String, Object>>> store = new HashMap<>();
    private final ModuleRecordService records = mock(ModuleRecordService.class);
    private final BusinessLoopService events = mock(BusinessLoopService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class, invocation -> {
        if (invocation.getMethod().getName().equals("queryForList")) return List.of(Map.of("id", 9L));
        if (invocation.getMethod().getName().equals("update")) return 1;
        return RETURNS_DEFAULTS.answer(invocation);
    });
    private final ActivationCodeController controller = new ActivationCodeController(records, events, jdbc);
    @BeforeEach
    void setup() {
        when(records.listModuleRecords(anyString())).thenAnswer(call -> store.getOrDefault(call.getArgument(0), List.of()));
        when(records.replaceModuleRecords(anyString(), anyList())).thenAnswer(call -> { String key = call.getArgument(0); List<Map<String, Object>> rows = new ArrayList<>(call.getArgument(1)); store.put(key, rows); return rows; });
    }

    @Test
    void generationValidatesLimitsAndProducesUniqueCodesHashesAndAuditEntries() {
        for (Map<String, Object> payload : List.<Map<String, Object>>of(Map.of("deviceLimit", 0), Map.of("deviceLimit", 10001), Map.of("quantity", 0), Map.of("quantity", 101))) {
            assertEquals(-1, controller.createActivationCodes(payload).getCode());
        }
        var result = data(controller.generateActivationCode(Map.of("quantity", 3, "deviceLimit", 2, "owner", "信息中心", "expiresAt", "2035-01-01")));
        var codes = maps(result.get("codes"));
        assertEquals(3, codes.size());
        assertEquals(3, codes.stream().map(row -> row.get("code")).distinct().count());
        assertTrue(codes.stream().allMatch(row -> row.get("code").toString().matches("[a-f0-9]{8}(-[a-f0-9]{8}){2}")));
        var persisted = store.get("activation-codes");
        assertEquals(3, persisted.size());
        assertEquals(64, persisted.get(0).get("codeHash").toString().length());
        assertEquals("信息中心", persisted.get(0).get("ownerName"));
        assertEquals(2, maps(result.get("records")).get(0).get("availableCount"));
        assertEquals(3, maps(controller.listActivationAuditLogs().getData()).size());
        assertEquals(200, controller.createActivationCodes(null).getCode());
    }

    @Test
    void listFiltersDeletedExpiredQuotaAndKeywordAndReturnsStatistics() {
        var active = code("active"); active.put("ownerName", "中心");
        var disabled = code("disabled"); disabled.put("enabled", false);
        var expired = code("expired"); expired.put("expireAt", "2020-01-01T00:00:00");
        var full = code("full"); full.put("deviceLimit", 1); full.put("boundDevices", List.of(binding("machine", "ACTIVE")));
        var deleted = code("deleted"); deleted.put("deleted", true);
        var soon = code("soon"); soon.put("expireAt", LocalDateTime.now().plusDays(3).toString());
        put(active, disabled, expired, full, deleted, soon);
        assertEquals(5, data(controller.listActivationCodes(null)).get("total"));
        assertEquals(1, listed(Map.of("keyword", "中心")).size());
        assertEquals(1, listed(Map.of("status", "停用")).size());
        assertEquals(1, listed(Map.of("status", "expired")).size());
        assertEquals(1, listed(Map.of("status", "额度已满")).size());
        assertEquals(1, listed(Map.of("expireStatus", "expiring_soon")).size());
        assertEquals(1, listed(Map.of("expireStatus", "expired")).size());
        assertEquals(3, listed(Map.of("expireStatus", "long_term")).size());
        assertEquals(1, listed(Map.of("page", "2", "size", "1")).size());
        assertEquals(0, listed(Map.of("page", "99", "size", "1")).size());
        var statistics = map(data(controller.listActivationCodes(Map.of())).get("statistics"));
        assertEquals(5, statistics.get("totalCodes")); assertEquals(1, statistics.get("disabledCodes"));
        assertEquals(1, statistics.get("boundDevices"));
    }

    @Test
    void labBindingChecksAvailabilityAndUnbindClearsLabMetadata() {
        var first = code("first"); var second = code("second"); put(first, second);
        assertError(controller.bindActivationCodeToLab(null));
        assertError(controller.bindActivationCodeToLab(Map.of("code", "first")));
        assertError(controller.bindActivationCodeToLab(Map.of("code", "missing", "labId", 7)));
        var payload = Map.<String, Object>of("code", "first", "labId", 7, "labCode", "A101", "labName", "机房");
        assertEquals(200, controller.bindActivationCodeToLab(payload).getCode());
        assertEquals("7", first.get("labId"));
        assertEquals(1, maps(controller.listLabActivationBindings().getData()).size());
        assertError(controller.bindActivationCodeToLab(Map.of("code", "second", "labId", 7)));
        assertError(controller.bindActivationCodeToLab(Map.of("code", "first", "labId", 8)));
        assertEquals(200, controller.unbindActivationCodeFromLab("first").getCode());
        assertEquals("", first.get("labId"));
        assertEquals(0, maps(controller.listLabActivationBindings().getData()).size());
        first.put("enabled", false); assertError(controller.bindActivationCodeToLab(payload));
        first.put("enabled", true); first.put("expireAt", "2020-01-01T00:00:00"); assertError(controller.bindActivationCodeToLab(payload));
        assertError(controller.unbindActivationCodeFromLab("missing"));
    }

    @Test
    void statusRemarkAndBatchOperationsOnlyAffectRequestedRecords() {
        var first = code("first"); var second = code("second"); put(first, second);
        assertEquals(200, controller.disableActivationCode("first").getCode());
        assertEquals(false, first.get("enabled"));
        controller.updateActivationCodeStatus("first", Map.of("enabled", true, "reason", "恢复"));
        assertEquals(true, first.get("enabled"));
        controller.updateActivationCodeRemark("first", Map.of("remark", "教学使用", "owner", "教务处", "expiresAt", "2035-01-01 12:00:00"));
        assertEquals("教学使用", first.get("remark")); assertEquals("教务处", first.get("ownerName"));
        assertEquals("2035-01-01T12:00:00", first.get("expireAt"));
        for (String action : List.of("DISABLE", "ENABLE", "UPDATE_OWNER", "EXTEND_EXPIRE_AT", "DELETE")) {
            var result = data(controller.batchOperateActivationCodes(Map.of("ids", List.of("first"), "action", action, "ownerName", "管理员", "expireAt", "2036-01-01")));
            assertEquals(1, result.get("affected"));
            assertFalse(second.containsKey("deleted"));
        }
        assertEquals(true, first.get("deleted"));
        assertError(controller.batchOperateActivationCodes(null));
        assertError(controller.batchOperateActivationCodes(Map.of("ids", List.of("first"))));
        assertError(controller.batchOperateActivationCodes(Map.of("ids", List.of("first"), "action", "UNKNOWN")));
        assertError(controller.updateActivationCodeStatus("missing", null));
        assertError(controller.updateActivationCodeRemark("missing", null));
    }

    @Test
    void verificationDistinguishesInvalidDisabledExpiredAndQuotaFailures() {
        var value = code("first"); put(value);
        assertEquals("invalid", data(controller.verifyActivationCode(null)).get("status"));
        assertEquals("invalid", data(controller.verifyActivationCode(Map.of("code", "missing", "machineCode", "machine"))).get("status"));
        value.put("enabled", false); assertEquals("disabled", verify("first", "machine").get("status"));
        value.put("enabled", true); value.put("expireAt", "2020-01-01T00:00:00"); assertEquals("expired", verify("first", "machine").get("status"));
        value.remove("expireAt"); value.put("deviceLimit", 1);
        assertEquals(true, verify("first", "machine").get("valid"));
        assertEquals("quota_exceeded", verify("first", "other").get("status"));
        var bindings = maps(controller.listActivationCodeBindings("first").getData());
        assertEquals(1, bindings.size());
        assertEquals(200, controller.remoteUnbindActivationDevice("first", bindings.get(0).get("id").toString(), Map.of("reason", "设备报废")).getCode());
        assertEquals("remote_unbound", verify("first", "machine").get("status"));
        assertError(controller.listActivationCodeBindings("missing"));
    }

    @Test
    void repeatedVerificationReusesTerminalCredentialsAndWritesSplitTablesForBoundLabs() {
        var value = code("first"); value.put("labId", 7); value.put("labCode", "A101"); value.put("labName", "机房"); put(value);
        var request = Map.<String, Object>of("code", " FIRST ", "machineCode", " HOST-001 ", "macAddress", "001122334455", "hostname", "电脑一", "clientSource", "teacher", "clientVersion", "1.0");
        var first = data(controller.verifyActivationCode(request));
        var second = data(controller.verifyActivationCode(request));
        assertEquals(true, first.get("valid"));
        assertEquals("host-001", first.get("machineCode"));
        assertEquals("00:11:22:33:44:55", first.get("macAddress"));
        assertEquals(first.get("terminalId"), second.get("terminalId"));
        assertEquals(first.get("terminalToken"), second.get("terminalToken"));
        assertTrue(first.get("terminalToken").toString().startsWith("tt-host-001-"));
        assertEquals(1, maps(value.get("boundDevices")).size());
        assertTrue(mockingDetails(jdbc).getInvocations().stream().anyMatch(call -> call.getArgument(0).toString().contains("insert into lab_terminal")));
        assertEquals(1, maps(controller.listActivationAuditLogs().getData()).size());
    }

    @Test
    void remoteBindingRejectsMissingOrUnavailableTargetsAndQuotaOverflow() {
        var value = code("first"); value.put("deviceLimit", 1); put(value);
        assertError(controller.remoteBindActivationDevice("first", null));
        assertError(controller.remoteBindActivationDevice("missing", Map.of("machineCode", "host")));
        value.put("enabled", false); assertError(controller.remoteBindActivationDevice("first", Map.of("machineCode", "host")));
        value.put("enabled", true); value.put("expireAt", "2020-01-01T00:00:00"); assertError(controller.remoteBindActivationDevice("first", Map.of("machineCode", "host")));
        value.remove("expireAt");
        assertEquals(200, controller.remoteBindActivationDevice("first", Map.of("machineCode", "host", "macAddress", "001122334455", "hostname", "电脑")).getCode());
        assertError(controller.remoteBindActivationDevice("first", Map.of("machineCode", "host")));
        assertError(controller.remoteBindActivationDevice("first", Map.of("machineCode", "other")));
        assertError(controller.remoteUnbindActivationDevice("first", "host", null));
        assertError(controller.remoteUnbindActivationDevice("missing", "host", Map.of("reason", "报废")));
        assertError(controller.remoteUnbindActivationDevice("first", "missing", Map.of("reason", "报废")));
    }

    @Test
    void forcedMigrationInvalidatesOldBindingBeforeActivatingNewCode() {
        var first = code("first"); first.put("boundDevices", List.of(binding("host", "ACTIVE")));
        var second = code("second"); put(first, second);
        var conflict = data(controller.remoteBindActivationDevice("second", Map.of("machineCode", "host")));
        assertEquals(true, conflict.get("conflict"));
        assertEquals(200, controller.remoteBindActivationDevice("second", Map.of("machineCode", "host", "force", true)).getCode());
        var previousBinding = maps(controller.listActivationCodeBindings("first").getData()).get(0);
        assertEquals("REMOTE_UNBOUND", previousBinding.get("status"));
        assertEquals("remote_unbound", verify("first", "host").get("status"));
        assertEquals(true, verify("second", "host").get("valid"));
    }

    @Test
    void legacyMacBindingsAndExpiryFormatsRemainReadable() {
        var value = code("legacy"); value.remove("deviceLimit"); value.put("macAddress", "001122334455"); value.put("hostName", "旧电脑"); value.put("expiresAt", "2035-01-01"); put(value);
        var bindings = maps(controller.listActivationCodeBindings("legacy").getData());
        assertEquals(1, bindings.size()); assertEquals("00:11:22:33:44:55", bindings.get(0).get("macAddress"));
        assertEquals(true, data(controller.verifyActivationCode(Map.of("code", "legacy", "macAddress", "00-11-22-33-44-55"))).get("valid"));
        value.put("expireAt", "bad-date");
        assertEquals("long_term", listed(Map.of()).get(0).get("expireStatus"));
    }

    @Test
    void legacyStatusAliasesAndBooleanInputsKeepFilteringAndCreationConsistent() {
        assertEquals(0, data(controller.listActivationCodes(Map.of())).get("total"));
        for (Object enabled : List.of(false, "", "false", "true", "1", "yes", "enabled")) {
            var generated = data(controller.createActivationCodes(Map.of("enabled", enabled, "deviceLimit", "2", "quantity", "1")));
            boolean expected = !List.of(false, "", "false").contains(enabled);
            assertEquals(expected, map(generated.get("record")).get("enabled"));
        }
        var legacy = code("legacy"); legacy.remove("enabled"); legacy.put("status", "active"); put(legacy);
        for (String status : List.of("active", "enabled", "启用", "有效", "all", "")) {
            assertEquals(1, listed(Map.of("status", status, "expireStatus", "all")).size());
        }
        for (String status : List.of("disabled", "deleted")) {
            legacy.put("status", status);
            assertEquals("disabled", verify("legacy", "host").get("status"));
        }
        legacy.put("status", "active"); legacy.put("deviceLimit", 0);
        assertEquals(1, listed(Map.of("status", "enabled")).size());
        assertEquals(0, listed(Map.of("status", "unknown")).size());
    }

    @Test
    void labConflictsMatchCodesAndNamesAndIgnoreDeletedOrDisabledRecords() {
        var target = code("target"); target.put("id", "record-target"); target.put("remark", "kept");
        var other = code("other"); other.put("labId", "8"); other.put("labCode", "A101"); other.put("labName", "实验室");
        var deleted = code("deleted"); deleted.put("deleted", true); put(target, other, deleted);
        assertError(controller.bindActivationCodeToLab(Map.of("code", "target", "labId", "7", "labCode", "A101")));
        assertError(controller.bindActivationCodeToLab(Map.of("code", "target", "labId", "7", "labName", "实验室")));
        other.put("enabled", false);
        var request = Map.<String, Object>of("code", "target", "labId", "7", "labCode", "A101", "labName", "实验室");
        assertEquals(200, controller.bindActivationCodeToLab(request).getCode());
        assertEquals(200, controller.bindActivationCodeToLab(request).getCode());
        assertEquals(200, controller.updateActivationCodeRemark("target", null).getCode());
        assertEquals("kept", target.get("remark"));
        assertEquals(200, controller.updateActivationCodeRemark("target", Map.of("ownerName", "新管理员", "expireAt", "2035-02-01T12:00:00")).getCode());
        assertEquals("新管理员", target.get("ownerName"));
        target.put("maskedCode", "MASKED");
        assertEquals(200, controller.listActivationCodeBindings("MASKED").getCode());
        assertError(controller.listActivationCodeBindings(" "));
        assertError(controller.bindActivationCodeToLab(Map.of("code", "deleted", "labId", "7")));
        assertError(controller.unbindActivationCodeFromLab("deleted"));
        assertEquals("invalid", verify("deleted", "host").get("status"));
        assertEquals(1, data(controller.batchOperateActivationCodes(Map.of("ids", "target", "action", "ENABLE", "reason", "恢复可用"))).get("affected"));
    }

    @Test
    void malformedStoredBindingsAreDiscardedAndRemoteUnbindAcceptsEveryPublishedIdentifier() {
        var value = code("first");
        var binding = binding("host", "UNBOUND"); binding.put("deviceId", "old-device"); binding.put("macAddress", "001122334455");
        value.put("boundDevices", List.of("broken-row", Map.of(), binding)); put(value);
        assertEquals(1, maps(controller.listActivationCodeBindings("first").getData()).size());
        assertEquals(1, listed(Map.of("keyword", "old-device")).size());
        for (String identifier : List.of("host", "old-device", "00:11:22:33:44:55")) {
            assertEquals(200, controller.remoteUnbindActivationDevice("first", identifier, Map.of("reason", "设备退役")).getCode());
        }
        var statistics = map(data(controller.listActivationCodes(Map.of())).get("statistics"));
        assertNotNull(statistics);
        assertEquals("remote_unbound", data(controller.verifyActivationCode(Map.of("code", "first", "machineCode", "another", "macAddress", "001122334455"))).get("status"));
    }

    @Test
    void splitTableFailuresDoNotLoseLegacyActivationAndClientTypesArePreserved() {
        for (String source : List.of("probe", "student")) {
            var value = code("first"); value.put("labId", "7"); put(value);
            var response = data(controller.verifyActivationCode(Map.of("code", "first", "machineCode", "host", "clientSource", source)));
            assertEquals(true, response.get("valid"));
            assertTrue(mockingDetails(jdbc).getInvocations().stream().anyMatch(call -> call.getMethod().getName().equals("update") && call.getArguments().length > 8 && java.util.Arrays.asList(call.getArguments()).contains(source.equals("probe") ? "probe_client" : "lab_client")));
        }
        var value = code("fallback"); value.put("labId", "not-a-number"); put(value);
        assertEquals(true, verify("fallback", "host").get("valid"));
        value = code("fallback"); value.put("labId", "7"); put(value);
        doThrow(new IllegalStateException("split tables unavailable")).when(jdbc).update(anyString(), any(Object[].class));
        assertEquals(true, verify("fallback", "host").get("valid"));
        assertEquals(1, maps(controller.listActivationCodeBindings("fallback").getData()).size());
    }

    private Map<String, Object> code(String code) { var value = new LinkedHashMap<String, Object>(); value.put("id", code); value.put("code", code); value.put("enabled", true); value.put("deviceLimit", 2); value.put("createdAt", "2026-01-01T00:00:00"); return value; }
    private Map<String, Object> binding(String machine, String status) { return new LinkedHashMap<>(Map.of("id", "binding-" + machine, "machineCode", machine, "status", status, "boundAt", LocalDateTime.now().toString())); }
    @SafeVarargs private final void put(Map<String, Object>... values) { store.put("activation-codes", new ArrayList<>(List.of(values))); }
    private Map<String, Object> verify(String code, String machine) { return data(controller.verifyActivationCode(Map.of("code", code, "machineCode", machine))); }
    private List<Map<String, Object>> listed(Map<String, String> query) { return maps(data(controller.listActivationCodes(query)).get("records")); }
    private Map<String, Object> data(Result result) { assertEquals(200, result.getCode(), result.getMessage()); return map(result.getData()); }
    @SuppressWarnings("unchecked") private Map<String, Object> map(Object value) { return (Map<String, Object>) value; }
    @SuppressWarnings("unchecked") private List<Map<String, Object>> maps(Object value) { return (List<Map<String, Object>>) value; }
    private void assertError(Result result) { assertEquals(-1, result.getCode()); }
}
