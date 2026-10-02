package org.example.backend.controller;

import org.example.backend.entity.LabEntity;
import org.example.backend.service.DevicesService;
import org.example.backend.service.LabService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LabQueryCompatibilityTest {
    private LabService labService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        labService = mock(LabService.class);
        DevicesService devicesService = mock(DevicesService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LabController(labService, devicesService)).build();
    }

    @Test
    void preservesSystemAdministratorListContractAndAllDisplayFields() throws Exception {
        when(labService.getLabs(null)).thenReturn(List.of(lab()));

        mockMvc.perform(get("/labs").requestAttr("authRole", "systemAdmin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询实验室列表成功"))
                .andExpect(jsonPath("$.data[0].id").value(7))
                .andExpect(jsonPath("$.data[0].labCode").value("LAB-007"))
                .andExpect(jsonPath("$.data[0].labName").value("网络实验室"))
                .andExpect(jsonPath("$.data[0].building").value("实训楼"))
                .andExpect(jsonPath("$.data[0].floor").value("3"))
                .andExpect(jsonPath("$.data[0].roomNo").value("305"))
                .andExpect(jsonPath("$.data[0].labType").value("网络"))
                .andExpect(jsonPath("$.data[0].capacity").value(48))
                .andExpect(jsonPath("$.data[0].managerUserId").value(42))
                .andExpect(jsonPath("$.data[0].managerName").value("实验室管理员"))
                .andExpect(jsonPath("$.data[0].openStatus").value("开放"))
                .andExpect(jsonPath("$.data[0].deviceCount").value(36))
                .andExpect(jsonPath("$.data[0].remark").value("核心教学实验室"))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-03-01T08:00:00"))
                .andExpect(jsonPath("$.data[0].updatedAt").value("2026-09-01T09:30:00"));

        verify(labService).getLabs(null);
    }

    @Test
    void scopesNonSystemAdministratorListToAuthenticatedUser() throws Exception {
        when(labService.getLabs(42)).thenReturn(List.of(lab()));

        mockMvc.perform(get("/labs")
                        .requestAttr("authRole", "labAdmin")
                        .requestAttr("authUserId", 42))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].managerUserId").value(42));

        verify(labService).getLabs(42);
    }

    @Test
    void preservesDetailAndMissingLabContracts() throws Exception {
        when(labService.getLabById(7)).thenReturn(lab());
        when(labService.getLabById(999)).thenReturn(null);

        mockMvc.perform(get("/labs/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询实验室详情成功"))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.labCode").value("LAB-007"));

        mockMvc.perform(get("/labs/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询实验室详情成功"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private LabEntity lab() {
        LabEntity lab = new LabEntity();
        lab.setId(7);
        lab.setLabCode("LAB-007");
        lab.setLabName("网络实验室");
        lab.setBuilding("实训楼");
        lab.setFloor("3");
        lab.setRoomNo("305");
        lab.setLabType("网络");
        lab.setCapacity(48);
        lab.setManagerUserId(42);
        lab.setManagerName("实验室管理员");
        lab.setOpenStatus("开放");
        lab.setDeviceCount(36);
        lab.setRemark("核心教学实验室");
        lab.setCreatedAt(LocalDateTime.of(2026, 3, 1, 8, 0));
        lab.setUpdatedAt(LocalDateTime.of(2026, 9, 1, 9, 30));
        return lab;
    }
}
