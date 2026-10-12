package org.example.backend.controller;

import org.example.backend.config.GlobalExceptionHandler;
import org.example.backend.labresource.web.LabQueryController;
import org.example.backend.labresource.api.LabAccessScope;
import org.example.backend.labresource.api.LabCatalogQuery;
import org.example.backend.labresource.api.LabView;
import org.example.backend.platform.security.AuthenticationAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LabQueryCompatibilityTest {
    private LabCatalogQuery query;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        query = mock(LabCatalogQuery.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LabQueryController(query))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void preservesSystemAdministratorListContractAndAllDisplayFields() throws Exception {
        when(query.list(any())).thenReturn(List.of(lab()));

        mockMvc.perform(get("/labs").requestAttr(AuthenticationAttributes.ROLE, "systemAdmin"))
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

        verify(query).list(LabAccessScope.all());
    }

    @Test
    void scopesNonSystemAdministratorListToAuthenticatedUser() throws Exception {
        when(query.list(any())).thenReturn(List.of(lab()));

        mockMvc.perform(get("/labs")
                        .requestAttr(AuthenticationAttributes.ROLE, "labAdmin")
                        .requestAttr(AuthenticationAttributes.USER_ID, 42))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].managerUserId").value(42));

        verify(query).list(LabAccessScope.managedBy(42L));
    }

    @Test
    void preservesDetailAndMissingLabContracts() throws Exception {
        when(query.findById(eq(7L), any())).thenReturn(lab());
        when(query.findById(eq(999L), any())).thenReturn(null);

        mockMvc.perform(get("/labs/7")
                        .requestAttr(AuthenticationAttributes.ROLE, "systemAdmin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询实验室详情成功"))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.labCode").value("LAB-007"));

        mockMvc.perform(get("/labs/999")
                        .requestAttr(AuthenticationAttributes.ROLE, "systemAdmin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询实验室详情成功"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void preservesForbiddenStatusWhenAuthenticationIdentityIsMissing() throws Exception {
        mockMvc.perform(get("/labs")
                        .requestAttr(AuthenticationAttributes.ROLE, "labAdmin"))
                .andExpect(status().isForbidden());
    }

    private LabView lab() {
        return new LabView(
                7L, "LAB-007", "网络实验室", "实训楼", "3", "305", "网络", 48,
                42L, "实验室管理员", "开放", 36, "核心教学实验室",
                LocalDateTime.of(2026, 3, 1, 8, 0), LocalDateTime.of(2026, 9, 1, 9, 30)
        );
    }
}
