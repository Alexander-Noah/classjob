package org.example.backend.device;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceModuleBoundaryTest {
    @Test
    void globalExceptionFallbackIncludesRequestIdInsteadOfGenericRetryCopy() throws IOException {
        String handler = readSource("config", "GlobalExceptionHandler.java");
        assertAll(
                () -> assertFalse(handler.contains("服务器异常，请稍后重试")),
                () -> assertTrue(handler.contains("系统处理失败，请联系管理员")),
                () -> assertTrue(handler.contains("请求编号"))
        );
    }

    @Test
    void deviceInventoryOnlyUpdatesInventoryDateAndKeepsRuntimeStateOutOfAssets() throws IOException {
        String service = readSource("service", "impl", "DevicesServiceImpl.java");
        String mapper = readSource("mapper", "DevicesMapper.java");
        String mapperXml = readResource("mapper", "DevicesMapper.xml");
        String inventoryUpdateSql = between(mapperXml, "<update id=\"updateInventoryState\">", "</update>");
        assertAll(
                () -> assertTrue(service.contains("updateInventoryState(deviceId, inventoryDate)")),
                () -> assertFalse(service.contains("record.getResultStatus(), device.getHealth(), device.getOnline()")),
                () -> assertTrue(mapper.contains("@Param(\"inventoryDate\") String inventoryDate")),
                () -> assertFalse(inventoryUpdateSql.contains("status =")),
                () -> assertFalse(inventoryUpdateSql.contains("health =")),
                () -> assertFalse(inventoryUpdateSql.contains("online =")),
                () -> assertTrue(inventoryUpdateSql.contains("inventory_date = #{inventoryDate}"))
        );
    }

    @Test
    void repairSyncUpdatesOnlyAssetMaintenanceStateAndNeverRuntimeHeartbeatFields() throws IOException {
        String service = readSource("service", "impl", "BusinessLoopServiceImpl.java");
        String mapper = readSource("mapper", "DevicesMapper.java");
        String mapperXml = readResource("mapper", "DevicesMapper.xml");
        String repairUpdateSql = between(mapperXml, "<update id=\"updateDeviceAssetMaintenanceStateByNameOrCode\">", "</update>");
        assertAll(
                () -> assertTrue(service.contains("updateDeviceAssetMaintenanceStateByNameOrCode(")),
                () -> assertFalse(service.contains("updateDeviceRuntimeStateByNameOrCode(")),
                () -> assertTrue(mapper.contains("int updateDeviceAssetMaintenanceStateByNameOrCode(")),
                () -> assertTrue(mapperXml.contains("<update id=\"updateDeviceAssetMaintenanceStateByNameOrCode\">")),
                () -> assertFalse(repairUpdateSql.contains("health =")),
                () -> assertFalse(repairUpdateSql.contains("online ="))
        );
    }

    @Test
    void deviceAssetStatsIncludeIdleLifecycleStatus() throws IOException {
        String service = readSource("service", "impl", "DevicesServiceImpl.java");
        String mapperXml = readResource("mapper", "DevicesMapper.xml");
        String statsSql = between(mapperXml, "<select id=\"getDeviceStats\" resultType=\"map\">", "</select>");
        assertAll(
                () -> assertTrue(service.contains("normalized.put(\"idle\"")),
                () -> assertTrue(statsSql.contains(" as idle")),
                () -> assertTrue(statsSql.contains("status = '闲置'"))
        );
    }

    @Test
    void dashboardShortcutKeepsCoreAssetLifecycleEntry() throws IOException {
        String service = readSource("service", "impl", "DashboardServiceImpl.java");
        assertAll(
                () -> assertTrue(service.contains("资产台账与生命周期管理")),
                () -> assertFalse(service.contains("/lab-admin/device-status"))
        );
    }

    private String readSource(String... parts) throws IOException {
        Path path = Path.of("src", "main", "java", "org", "example", "backend");
        for (String part : parts) path = path.resolve(part);
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path);
    }

    private String readResource(String... parts) throws IOException {
        Path path = Path.of("src", "main", "resources");
        for (String part : parts) path = path.resolve(part);
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path);
    }

    private String between(String value, String startMarker, String endMarker) {
        int start = value.indexOf(startMarker);
        assertTrue(start >= 0, startMarker + " should exist");
        int end = value.indexOf(endMarker, start);
        assertTrue(end > start, endMarker + " should exist after " + startMarker);
        return value.substring(start, end + endMarker.length());
    }
}
