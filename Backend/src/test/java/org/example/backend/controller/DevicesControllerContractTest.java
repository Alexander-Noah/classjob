/*
 * 设备管理回归测试；测试夹具与生产配置分离。
 * 覆盖场景：listPageAndStatsKeepTheirEstablishedGetContracts、createReturnsTheAuthoritativeServiceDevice、updateAndBatchUpdateReturnTheAuthoritativeServiceDevices、deleteKeepsTheEstablishedDeleteBinding 等。
 * 主要协作组件：ObjectMapper。
 */
package org.example.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.entity.DeviceInventoryRecordEntity;
import org.example.backend.entity.DeviceTransferRecordEntity;
import org.example.backend.entity.DevicesEntity;
import org.example.backend.security.JwtAuthenticationFilter;
import org.example.backend.service.DevicesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DevicesController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@ContextConfiguration(classes = DevicesController.class)
class DevicesControllerContractTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private DevicesService devicesService;

    /**
     * 列出页码AndStatsKeepTheirEstablishedGetContracts。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void listPageAndStatsKeepTheirEstablishedGetContracts() throws Exception {
        DevicesEntity listed = device(1L, "DEV-001", "列表设备");
        DevicesService.DevicePageResult page = new DevicesService.DevicePageResult(List.of(listed), 1);
        when(devicesService.getDevices()).thenReturn(List.of(listed));
        when(devicesService.pageDevices(any())).thenReturn(page);
        when(devicesService.getDeviceStats()).thenReturn(Map.of("total", 1L, "normal", 1L));

        mockMvc.perform(get("/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].deviceCode").value("DEV-001"))
                .andExpect(jsonPath("$.data[0].assetNo").value("DEV-001"))
                .andExpect(jsonPath("$.data[0].code").value("DEV-001"))
                .andExpect(jsonPath("$.data[0].name").value("列表设备"))
                .andExpect(jsonPath("$.data[0].lab").value("兼容实验室"))
                .andExpect(jsonPath("$.data[0].roomNo").value("A-101"))
                .andExpect(jsonPath("$.data[0].specification").value("兼容规格"))
                .andExpect(jsonPath("$.data[0].health").value("良好"))
                .andExpect(jsonPath("$.data[0].online").value(true));

        mockMvc.perform(get("/devices/page")
                        .param("pageNum", "2")
                        .param("pageSize", "5")
                        .param("keyword", "列表"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].deviceName").value("列表设备"));

        mockMvc.perform(get("/devices/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.normal").value(1));

        verify(devicesService).pageDevices(any());
    }

    /**
     * 创建ReturnsTheAuthoritative服务设备。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void createReturnsTheAuthoritativeServiceDevice() throws Exception {
        DevicesEntity submitted = device(null, "client-code", "客户端名称");
        DevicesEntity authoritative = device(17L, "DB-017", "数据库名称");
        when(devicesService.InserterDevices(any())).thenReturn(authoritative);

        mockMvc.perform(post("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(submitted)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(17))
                .andExpect(jsonPath("$.data.deviceCode").value("DB-017"))
                .andExpect(jsonPath("$.data.deviceName").value("数据库名称"))
                .andExpect(jsonPath("$.data.assetNo").value("DB-017"))
                .andExpect(jsonPath("$.data.code").value("DB-017"))
                .andExpect(jsonPath("$.data.name").value("数据库名称"))
                .andExpect(jsonPath("$.data.lab").value("兼容实验室"))
                .andExpect(jsonPath("$.data.roomNo").value("A-101"))
                .andExpect(jsonPath("$.data.specification").value("兼容规格"))
                .andExpect(jsonPath("$.data.health").value("良好"))
                .andExpect(jsonPath("$.data.online").value(true));
    }

    /**
     * 更新AndBatchUpdateReturnTheAuthoritative服务设备。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void updateAndBatchUpdateReturnTheAuthoritativeServiceDevices() throws Exception {
        DevicesEntity submitted = device(9L, "client-code", "客户端名称");
        DevicesEntity authoritative = device(9L, "DB-009", "数据库名称");
        when(devicesService.updateDevices(eq(9L), any())).thenReturn(authoritative);
        when(devicesService.updateDevices(org.mockito.ArgumentMatchers.<List<DevicesEntity>>any()))
                .thenReturn(List.of(authoritative));

        mockMvc.perform(put("/devices/{id}", 9)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(submitted)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceCode").value("DB-009"))
                .andExpect(jsonPath("$.data.deviceName").value("数据库名称"));

        mockMvc.perform(put("/devices/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resource\":\"devices\",\"records\":[" + objectMapper.writeValueAsString(submitted) + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].deviceCode").value("DB-009"))
                .andExpect(jsonPath("$.data[0].deviceName").value("数据库名称"));
    }

    /**
     * 删除KeepsTheEstablishedDeleteBinding。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void deleteKeepsTheEstablishedDeleteBinding() throws Exception {
        doNothing().when(devicesService).deleteDevices(12L);

        mockMvc.perform(delete("/devices/{id}", 12))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("删除设备成功"));

        verify(devicesService).deleteDevices(12L);
    }

    /**
     * 库存RoutesReturnTheAuthoritative服务结果的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void inventoryRoutesReturnTheAuthoritativeServiceResult() throws Exception {
        DevicesEntity authoritative = device(7L, "DB-007", "盘点后的设备");
        DeviceInventoryRecordEntity record = new DeviceInventoryRecordEntity();
        record.setDeviceId(7L);
        record.setResultStatus("已盘点");
        DevicesService.DeviceInventoryRecordResult result =
                new DevicesService.DeviceInventoryRecordResult(authoritative, record, List.of(record));
        when(devicesService.listInventoryRecords(7L)).thenReturn(List.of(record));
        when(devicesService.recordDeviceInventory(eq(7L), any())).thenReturn(result);

        mockMvc.perform(get("/devices/{id}/inventory-records", 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].deviceId").value(7));

        mockMvc.perform(post("/devices/{id}/inventory", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(record)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.device.deviceCode").value("DB-007"))
                .andExpect(jsonPath("$.data.device.deviceName").value("盘点后的设备"));
    }

    /**
     * 调拨RoutesReturnTheAuthoritative服务结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void transferRoutesReturnTheAuthoritativeServiceResult() throws Exception {
        DevicesEntity authoritative = device(8L, "DB-008", "调拨后的设备");
        DeviceTransferRecordEntity record = new DeviceTransferRecordEntity();
        record.setDeviceId(8L);
        record.setToLocation("B201");
        DevicesService.DeviceTransferRecordResult result =
                new DevicesService.DeviceTransferRecordResult(authoritative, record, List.of(record));
        when(devicesService.listTransferRecords(8L)).thenReturn(List.of(record));
        when(devicesService.transferDevice(eq(8L), any())).thenReturn(result);

        mockMvc.perform(get("/devices/{id}/transfer-records", 8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].deviceId").value(8));

        mockMvc.perform(post("/devices/{id}/transfer", 8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(record)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.device.deviceCode").value("DB-008"))
                .andExpect(jsonPath("$.data.device.deviceName").value("调拨后的设备"));
    }

    /**
     * 设备的处理逻辑。
     * @param id 标识参数。
     * @param code 编码参数。
     * @param name 名称参数。
     * @return DevicesEntity 类型的处理结果。
     */
    private DevicesEntity device(Long id, String code, String name) {
        DevicesEntity device = new DevicesEntity();
        device.setId(id);
        device.setDeviceCode(code);
        device.setDeviceName(name);
        device.setLabName("兼容实验室");
        device.setLocation("A-101");
        device.setSpecs("兼容规格");
        device.setHealth("良好");
        device.setOnline(true);
        return device;
    }
}
