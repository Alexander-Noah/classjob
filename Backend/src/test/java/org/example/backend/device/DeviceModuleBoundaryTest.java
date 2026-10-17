/*
 * 设备管理回归测试；测试夹具与生产配置分离。
 * 覆盖场景：deviceStatusHasDedicatedRuntimeApisBackedByRuntimeTables、iotApisUseIotTablesInsteadOfAssetDevicesForLabDevices、iotControlUsesGatewayAdapterLayerAndDoesNotInlineMockLogic、globalExceptionFallbackKeepsRequestIdInDiagnosticsRatherThanUserCopy 等。
 */
package org.example.backend.device;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceModuleBoundaryTest {
    /**
     * 设备状态HasDedicated运行时APIBackedBy运行时Tables的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void deviceStatusHasDedicatedRuntimeApisBackedByRuntimeTables() throws IOException {
        String controller = readSource("controller", "DeviceStatusController.java");
        String service = readSource("service", "impl", "DeviceStatusServiceImpl.java");

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("@GetMapping(\"/device-status/labs/{labId:\\\\d+}\"")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("@GetMapping(\"/device-status/devices/{deviceId:\\\\d+}\"")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("@GetMapping(\"/device-status/events\"")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("@PostMapping(\"/device-status/report\"")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("@GetMapping(\"/device-status/summary\"")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("device_runtime_status")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("device_status_event")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(service.contains("from device d"))
        );
    }

    /**
     * 物联APIUse物联TablesInsteadOf资产设备For实验室设备的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void iotApisUseIotTablesInsteadOfAssetDevicesForLabDevices() throws IOException {
        String controller = readSource("controller", "IotHardwareController.java");
        String service = readSource("service", "impl", "IotDeviceServiceImpl.java");
        String hardwareService = readSource("service", "impl", "IotHardwareServiceImpl.java");
        String hardwareInterface = readSource("service", "IotHardwareService.java");

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("@GetMapping(\"/iot/devices\")")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("@GetMapping(\"/iot/labs/{labId:\\\\d+}/overview\")")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("@PostMapping(\"/iot/devices\")")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("@PutMapping(\"/iot/devices/{id:\\\\d+}\")")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("@DeleteMapping(\"/iot/devices/{id:\\\\d+}\")")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("@PostMapping(\"/iot/control\")")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("@GetMapping(\"/iot/command-logs\")")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("@GetMapping(\"/iot/environment/{labId:\\\\d+}\"")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("iotDeviceService.listLabDevices(labId)")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("iot_device")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("iot_command_log")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(service.contains("DevicesMapper")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(service.contains("getDevices()")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(hardwareService.contains("DevicesMapper")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(hardwareService.contains("devicesMapper.getDevices()")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(hardwareInterface.contains("getLabDevices("))
        );
    }

    /**
     * 物联ControlUses网关适配器LayerAndDoesNotInlineMockLogic的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void iotControlUsesGatewayAdapterLayerAndDoesNotInlineMockLogic() throws IOException {
        String adapter = readSource("service", "gateway", "GatewayAdapter.java");
        String mockAdapter = readSource("service", "gateway", "MockGatewayAdapter.java");
        String httpAdapter = readSource("service", "gateway", "HttpGatewayAdapter.java");
        String mqttAdapter = readSource("service", "gateway", "MqttGatewayAdapter.java");
        String commandService = readSource("service", "gateway", "IotGatewayCommandService.java");
        String iotService = readSource("service", "impl", "IotDeviceServiceImpl.java");
        String controller = readSource("controller", "IotHardwareController.java");

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(adapter.contains("GatewayCommandResult sendCommand(GatewayCommandRequest request)")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(adapter.contains("GatewayStatusResult queryStatus(GatewayCommandRequest request)")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mockAdapter.contains("模拟网关演示")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(httpAdapter.contains("待配置真实网关")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mqttAdapter.contains("待配置真实网关")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(commandService.contains("gatewayAdapterRegistry.resolve")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(commandService.contains("insert into iot_command_log")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(iotService.contains("IotGatewayCommandService")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(iotService.contains("mock_success\".equals(resultStatus)")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(iotService.contains("!configured || \"mock\".equalsIgnoreCase(protocol)")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(controller.contains("mock_success"))
        );
    }

    /**
     * global异常FallbackKeeps请求标识进入DiagnosticsRatherThan用户Copy的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void globalExceptionFallbackKeepsRequestIdInDiagnosticsRatherThanUserCopy() throws IOException {
        String handler = readSource("config", "GlobalExceptionHandler.java");
        String loggingFilter = readSource("config", "RequestLoggingFilter.java");

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(handler.contains("服务暂时不可用，请稍后重试")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(handler.contains("请联系管理员并提供请求编号")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(handler.contains("MDC.get(\"requestId\")")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(loggingFilter.contains("response.setHeader(\"X-Request-Id\", requestId)"))
        );
    }

    /**
     * AIAssistantCustomAPIHasManual连接Test端点AndBusiness错误的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void aiAssistantCustomApiHasManualConnectionTestEndpointAndBusinessErrors() throws IOException {
        String controller = readSource("controller", "AiAssistantConfigController.java");
        String contract = readSource("service", "AiAssistantUserConfigService.java");
        String service = readSource("service", "impl", "AiAssistantUserConfigServiceImpl.java");

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("@PostMapping(\"/ai-assistant/user-config/test\")")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(contract.contains("Map<String, Object> testConnection(Integer userId);")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("public Map<String, Object> testConnection(Integer userId)")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("RestClientException")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("第三方 AI 接口连接失败")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(service.contains("throw new IllegalStateException(\"AI 接口未返回可展示内容\")"))
        );
    }

    /**
     * 实验室AdminCanAccessSplit设备状态And物联API的处理逻辑。
     */
    @Test
    void labAdminCanAccessSplitDeviceStatusAndIotApis() {
        Object policy = newPolicy();

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(isAllowed(policy, "GET", "/device-status/labs/2", "labAdmin")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(isAllowed(policy, "POST", "/device-status/report", "labAdmin")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(isAllowed(policy, "GET", "/iot/labs/2/devices", "labAdmin")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(isAllowed(policy, "POST", "/iot/control", "labAdmin")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(isAllowed(policy, "GET", "/device-status/labs/2", "teacher")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(isAllowed(policy, "POST", "/iot/control", "teacher"))
        );
    }

    /**
     * 设备库存OnlyUpdates库存日期AndKeeps运行时状态离开Of资产的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void deviceInventoryOnlyUpdatesInventoryDateAndKeepsRuntimeStateOutOfAssets() throws IOException {
        String service = readSource("service", "impl", "DevicesServiceImpl.java");
        String mapper = readSource("mapper", "DevicesMapper.java");
        String mapperXml = readResource("mapper", "DevicesMapper.xml");
        String inventoryUpdateSql = between(mapperXml, "<update id=\"updateInventoryState\">", "</update>");

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("updateInventoryState(deviceId, inventoryDate)")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(service.contains("record.getResultStatus(), device.getHealth(), device.getOnline()")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mapper.contains("@Param(\"inventoryDate\") String inventoryDate")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(mapper.contains("@Param(\"status\") String status,\n            @Param(\"health\") String health,\n            @Param(\"online\") Boolean online,\n            @Param(\"inventoryDate\") String inventoryDate")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(inventoryUpdateSql.contains("status =")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(inventoryUpdateSql.contains("health =")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(inventoryUpdateSql.contains("online =")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(inventoryUpdateSql.contains("inventory_date = #{inventoryDate}"))
        );
    }

    /**
     * 设备LifecycleUsesConsolidated事件数据映射器的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void deviceLifecycleUsesConsolidatedEventMapper() throws IOException {
        String service = readSource("service", "impl", "DevicesServiceImpl.java");
        String mapper = readSource("mapper", "DeviceEventMapper.java");
        String mapperXml = readResource("mapper", "DeviceEventMapper.xml");

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("DeviceEventMapper")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(service.contains("DeviceInventoryRecordMapper")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(service.contains("DeviceTransferRecordMapper")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mapper.contains("insertInventoryEvent")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mapper.contains("insertTransferEvent")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mapper.contains("listInventoryEvents")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mapper.contains("listTransferEvents")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mapperXml.contains("from device_event")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(mapperXml.contains("device_inventory_record")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(mapperXml.contains("device_transfer_record"))
        );
    }

    /**
     * repairSyncUpdatesOnly资产维护状态AndNever运行时HeartbeatFields的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void repairSyncUpdatesOnlyAssetMaintenanceStateAndNeverRuntimeHeartbeatFields() throws IOException {
        String service = readSource("service", "impl", "BusinessLoopServiceImpl.java");
        String mapper = readSource("mapper", "DevicesMapper.java");
        String mapperXml = readResource("mapper", "DevicesMapper.xml");
        String repairUpdateSql = mapperXml.contains("<update id=\"updateDeviceAssetMaintenanceStateByNameOrCode\">")
                ? between(mapperXml, "<update id=\"updateDeviceAssetMaintenanceStateByNameOrCode\">", "</update>")
                : between(mapperXml, "<update id=\"updateDeviceRuntimeStateByNameOrCode\">", "</update>");

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("updateDeviceAssetMaintenanceStateByNameOrCode(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(service.contains("updateDeviceRuntimeStateByNameOrCode(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(service.contains("boolean online")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(service.contains("\"health\", health")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mapper.contains("int updateDeviceAssetMaintenanceStateByNameOrCode(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(mapper.contains("int updateDeviceRuntimeStateByNameOrCode(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mapperXml.contains("<update id=\"updateDeviceAssetMaintenanceStateByNameOrCode\">")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(repairUpdateSql.contains("health =")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(repairUpdateSql.contains("online ="))
        );
    }

    /**
     * 设备资产StatsIncludeIdleLifecycle状态的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void deviceAssetStatsIncludeIdleLifecycleStatus() throws IOException {
        String service = readSource("service", "impl", "DevicesServiceImpl.java");
        String mapperXml = readResource("mapper", "DevicesMapper.xml");
        String statsSql = between(mapperXml, "<select id=\"getDeviceStats\" resultType=\"map\">", "</select>");

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("normalized.put(\"idle\"")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(statsSql.contains(" as idle")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(statsSql.contains("status = '闲置'"))
        );
    }

    /**
     * dashboardShortcutKeeps资产And运行时ConceptsSeparated的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void dashboardShortcutKeepsAssetAndRuntimeConceptsSeparated() throws IOException {
        String service = readSource("service", "impl", "DashboardServiceImpl.java");

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(service.contains("资产台账与设备状态")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("资产台账与生命周期管理"))
        );
    }

    /**
     * 运行时ReportCanResolveTerminalBy标识Or令牌的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void runtimeReportCanResolveTerminalByIdOrToken() throws IOException {
        String service = readSource("service", "impl", "DeviceStatusServiceImpl.java");

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("stringValue(request.get(\"terminalToken\"))")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("terminal_token_hash = ?")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("hashTerminalToken(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("MessageDigest.getInstance(\"SHA-256\")")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("StandardCharsets.UTF_8"))
        );
    }

    /**
     * 运行时ReportMaintainsTerminalFirstAndLastSeen时间的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void runtimeReportMaintainsTerminalFirstAndLastSeenTime() throws IOException {
        String service = readSource("service", "impl", "DeviceStatusServiceImpl.java");

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("first_connected_at = coalesce(first_connected_at, ?)")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("last_seen_at = ?"))
        );
    }

    /**
     * activation编码BindingPersists实验室编码AndTerminal凭据目标SplitTables的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void activationCodeBindingPersistsLabCodeAndTerminalCredentialsToSplitTables() throws IOException {
        String controller = readSource("controller", "ActivationCodeController.java");

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("JdbcTemplate jdbcTemplate")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("upsertLabActivationCode(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("upsertLabTerminalBinding(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("insert into lab_activation_code")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("insert into lab_terminal")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("terminal_token_hash")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("hashTerminalToken(")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("result.put(\"terminalId\"")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(controller.contains("result.put(\"terminalToken\""))
        );
    }

    /**
     * 读取来源。
     * @return String 类型的处理结果。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    private String readSource(String... parts) throws IOException {
        Path path = resolveModulePath("src", "main", "java", "org", "example", "backend", parts);
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path);
    }

    /**
     * 读取资源。
     * @return String 类型的处理结果。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    private String readResource(String... parts) throws IOException {
        Path path = resolveModulePath("src", "main", "resources", parts);
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path);
    }

    /**
     * 解析Module路径。
     * @param first first参数。
     * @param second second参数。
     * @param third third参数。
     * @param fourth fourth参数。
     * @param fifth fifth参数。
     * @param sixth sixth参数。
     * @return Path 类型的处理结果。
     */
    private Path resolveModulePath(String first, String second, String third, String fourth, String fifth,
                                   String sixth, String... remaining) {
        Path modulePath = Path.of(first, second, third, fourth, fifth, sixth);
        for (String part : remaining) {
            modulePath = modulePath.resolve(part);
        }
        return modulePath;
    }

    /**
     * 解析Module路径。
     * @param first first参数。
     * @param second second参数。
     * @param third third参数。
     * @return Path 类型的处理结果。
     */
    private Path resolveModulePath(String first, String second, String third, String... remaining) {
        Path modulePath = Path.of(first, second, third);
        for (String part : remaining) {
            modulePath = modulePath.resolve(part);
        }
        return modulePath;
    }

    /**
     * between的处理逻辑。
     * @param value 值参数。
     * @param startMarker 开始Marker参数。
     * @param endMarker 结束Marker参数。
     * @return String 类型的处理结果。
     */
    private String between(String value, String startMarker, String endMarker) {
        int start = value.indexOf(startMarker);
        assertTrue(start >= 0, startMarker + " should exist");
        int end = value.indexOf(endMarker, start);
        assertTrue(end > start, endMarker + " should exist after " + startMarker);
        return value.substring(start, end + endMarker.length());
    }

    /**
     * newPolicy的处理逻辑。
     * @return Object 类型的处理结果。
     */
    private Object newPolicy() {
        try {
            return Class.forName("org.example.backend.security.SecurityAccessPolicy")
                    .getConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * 判断是否为Allowed。
     * @param policy policy参数。
     * @param method 方法参数。
     * @param path 路径参数。
     * @param roleCode 角色编码参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isAllowed(Object policy, String method, String path, String roleCode) {
        try {
            return (Boolean) policy.getClass()
                    .getMethod("isAllowed", String.class, String.class, String.class)
                    .invoke(policy, method, path, roleCode);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
