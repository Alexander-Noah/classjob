package org.example.backend.labresource.adapter.in.web;

import org.example.backend.labresource.api.LabAccessScope;
import org.example.backend.labresource.api.LabCatalogQuery;
import org.example.backend.labresource.api.LabView;
import org.example.backend.platform.security.AuthenticationAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LabQueryControllerTest {
    private LabCatalogQuery query;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        query = mock(LabCatalogQuery.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LabQueryController(query)).build();
    }

    @Test
    void exposesCompatibleListResponseAndSystemAdministratorScope() throws Exception {
        when(query.list(any())).thenReturn(List.of(lab()));

        mockMvc.perform(get("/labs")
                        .requestAttr(AuthenticationAttributes.ROLE, "systemAdmin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询实验室列表成功"))
                .andExpect(jsonPath("$.data[0].id").value(7))
                .andExpect(jsonPath("$.data[0].labCode").value("LAB-007"))
                .andExpect(jsonPath("$.data[0].managerName").value("实验室管理员"))
                .andExpect(jsonPath("$.data[0].deviceCount").value(36));

        ArgumentCaptor<LabAccessScope> scope = ArgumentCaptor.forClass(LabAccessScope.class);
        verify(query).list(scope.capture());
        assertEquals(LabAccessScope.all(), scope.getValue());
    }

    @Test
    void scopesNonSystemAdministratorToAuthenticatedUser() throws Exception {
        when(query.list(any())).thenReturn(List.of(lab()));

        mockMvc.perform(get("/labs")
                        .requestAttr(AuthenticationAttributes.ROLE, "labAdmin")
                        .requestAttr(AuthenticationAttributes.USER_ID, 42))
                .andExpect(status().isOk());

        ArgumentCaptor<LabAccessScope> scope = ArgumentCaptor.forClass(LabAccessScope.class);
        verify(query).list(scope.capture());
        assertEquals(LabAccessScope.managedBy(42L), scope.getValue());
    }

    @Test
    void exposesCompatibleDetailResponse() throws Exception {
        when(query.findById(eq(7L), any())).thenReturn(lab());

        mockMvc.perform(get("/labs/7")
                        .requestAttr(AuthenticationAttributes.ROLE, "labAdmin")
                        .requestAttr(AuthenticationAttributes.USER_ID, 42))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询实验室详情成功"))
                .andExpect(jsonPath("$.data.id").value(7));

        verify(query).findById(7L, LabAccessScope.managedBy(42L));
    }

    @Test
    void rejectsMissingOrMalformedUserIdentityInsteadOfFallingBackToAllLabs() throws Exception {
        mockMvc.perform(get("/labs")
                        .requestAttr(AuthenticationAttributes.ROLE, "labAdmin"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/labs")
                        .requestAttr(AuthenticationAttributes.ROLE, "labAdmin")
                        .requestAttr(AuthenticationAttributes.USER_ID, "not-a-number"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(query);
    }

    private LabView lab() {
        return new LabView(
                7L, "LAB-007", "网络实验室", "实训楼", "3", "305", "网络", 48,
                42L, "实验室管理员", "开放", 36, "核心教学实验室",
                LocalDateTime.of(2026, 3, 1, 8, 0), LocalDateTime.of(2026, 9, 1, 9, 30)
        );
    }
}
