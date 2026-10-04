package org.example.backend.reservation.adapter.in.web;

import org.example.backend.platform.security.AuthenticationAttributes;
import org.example.backend.reservation.api.ReservationAccessScope;
import org.example.backend.reservation.api.ReservationActor;
import org.example.backend.reservation.api.ReservationCatalogQuery;
import org.example.backend.reservation.api.ReservationView;
import org.example.backend.reservation.application.CreateReservationCommand;
import org.example.backend.reservation.application.ReservationCommandHandler;
import org.example.backend.reservation.application.ReviewReservationCommand;
import org.example.backend.reservation.domain.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationControllerTest {
    private ReservationCatalogQuery catalog;
    private ReservationCommandHandler commands;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        catalog = mock(ReservationCatalogQuery.class);
        commands = mock(ReservationCommandHandler.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ReservationController(catalog, commands)).build();
    }

    @Test
    void preservesListContractAndEveryTaskOneCompatibilityField() throws Exception {
        when(catalog.list(any())).thenReturn(List.of(reservation()));

        assertReservationFields(mockMvc.perform(get("/reservations").requestAttr(AuthenticationAttributes.USER_ID, "42")
                        .requestAttr(AuthenticationAttributes.ROLE, "teacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("获取预约列表成功")), "$.data[0]");

        verify(catalog).list(ReservationAccessScope.forActor(ReservationActor.teacher(42L)));
    }

    @Test
    void preservesDetailContractAndMissingDetailAsSuccessfulNull() throws Exception {
        when(catalog.findById(eq(7L), any())).thenReturn(reservation());
        assertReservationFields(mockMvc.perform(get("/reservations/7").requestAttr(AuthenticationAttributes.USER_ID, 42)
                        .requestAttr(AuthenticationAttributes.ROLE, "systemAdmin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("获取预约详情成功")), "$.data");

        when(catalog.findById(eq(999L), any())).thenReturn(null);
        mockMvc.perform(get("/reservations/999").requestAttr(AuthenticationAttributes.USER_ID, 42)
                        .requestAttr(AuthenticationAttributes.ROLE, "systemAdmin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("获取预约详情成功"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void acceptsLegacyCreateAliasesAndReturnsOnlyAuthoritativeCommandResult() throws Exception {
        ReservationView authoritative = reservation();
        when(commands.create(any(), any())).thenReturn(authoritative);

        assertReservationFields(mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr(AuthenticationAttributes.USER_ID, 42)
                        .requestAttr(AuthenticationAttributes.ROLE, "teacher")
                        .requestAttr(AuthenticationAttributes.USERNAME, "刘老师")
                        .content("""
                                {
                                  "applicant": "别名申请人",
                                  "department": "软件学院",
                                  "contact": "刘老师",
                                  "labId": 9,
                                  "date": "2026-04-23",
                                  "timeRange": "08:00-10:00",
                                  "scene": "课程",
                                  "attendees": 68,
                                  "reason": "云计算平台实训课程",
                                  "status": "已通过",
                                  "conflictFlag": 1,
                                  "applicantUserId": 999,
                                  "reviewerName": "伪造审核人",
                                  "createdAt": "2020-01-01T01:01:01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("新增预约成功")), "$.data");

        ArgumentCaptor<CreateReservationCommand> command = ArgumentCaptor.forClass(CreateReservationCommand.class);
        ArgumentCaptor<ReservationActor> actor = ArgumentCaptor.forClass(ReservationActor.class);
        verify(commands).create(command.capture(), actor.capture());
        assertEquals(new CreateReservationCommand("别名申请人", "软件学院", "刘老师", 9L,
                LocalDate.of(2026, 4, 23), "08:00-10:00", "课程", 68, "云计算平台实训课程"), command.getValue());
        assertEquals(ReservationActor.teacher(42L, "刘老师"), actor.getValue());
    }

    @Test
    void acceptsOptionalReviewBodyAliasAndReturnsAuthoritativeReviewResult() throws Exception {
        when(commands.approve(any(), any())).thenReturn(reservation());
        when(commands.reject(any(), any())).thenReturn(reservation());

        mockMvc.perform(post("/reservations/7/approve")
                        .requestAttr(AuthenticationAttributes.USER_ID, 9)
                        .requestAttr(AuthenticationAttributes.ROLE, "labAdmin")
                        .requestAttr(AuthenticationAttributes.USERNAME, "张明")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"  门禁已同步放行。  \",\"reservationId\":999,\"reviewerName\":\"伪造审核人\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("通过预约成功"))
                .andExpect(jsonPath("$.data.id").value(7));

        mockMvc.perform(post("/reservations/7/reject")
                        .requestAttr(AuthenticationAttributes.USER_ID, 9)
                        .requestAttr(AuthenticationAttributes.ROLE, "systemAdmin")
                        .requestAttr(AuthenticationAttributes.USERNAME, "系统") )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("驳回预约成功"));

        verify(commands).approve(new ReviewReservationCommand(7L, "  门禁已同步放行。  "),
                ReservationActor.laboratoryAdministrator(9L, "张明"));
        verify(commands).reject(new ReviewReservationCommand(7L, null),
                ReservationActor.systemAdministrator(9L, "系统"));
    }

    @Test
    void failsClosedForMissingMalformedOrUnknownActorAttributes() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/reservations").requestAttr(AuthenticationAttributes.USER_ID, "bad")
                        .requestAttr(AuthenticationAttributes.ROLE, "teacher"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/reservations").requestAttr(AuthenticationAttributes.USER_ID, 0)
                        .requestAttr(AuthenticationAttributes.ROLE, "teacher"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/reservations").requestAttr(AuthenticationAttributes.USER_ID, 7)
                        .requestAttr(AuthenticationAttributes.ROLE, "visitor"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/reservations/7/approve").requestAttr(AuthenticationAttributes.USER_ID, 7)
                        .requestAttr(AuthenticationAttributes.ROLE, "labAdmin"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/reservations/7/reject").requestAttr(AuthenticationAttributes.USER_ID, 7)
                        .requestAttr(AuthenticationAttributes.ROLE, "labAdmin")
                        .requestAttr(AuthenticationAttributes.USERNAME, "  "))
                .andExpect(status().isForbidden());

        verifyNoInteractions(catalog, commands);
    }

    private void assertReservationFields(ResultActions result, String path) throws Exception {
        result
                .andExpect(jsonPath(path + ".id").value(7))
                .andExpect(jsonPath(path + ".applicantUserId").value(42))
                .andExpect(jsonPath(path + ".applicantName").value("软件工程 2 班"))
                .andExpect(jsonPath(path + ".applicant").value("软件工程 2 班"))
                .andExpect(jsonPath(path + ".department").value("软件学院"))
                .andExpect(jsonPath(path + ".contact").value("刘老师"))
                .andExpect(jsonPath(path + ".labId").value(9))
                .andExpect(jsonPath(path + ".reservationDate").value("2026-04-23"))
                .andExpect(jsonPath(path + ".date").value("2026-04-23"))
                .andExpect(jsonPath(path + ".timeRange").value("08:00-10:00"))
                .andExpect(jsonPath(path + ".scene").value("课程"))
                .andExpect(jsonPath(path + ".attendeeCount").value(68))
                .andExpect(jsonPath(path + ".attendees").value(68))
                .andExpect(jsonPath(path + ".reason").value("云计算平台实训课程"))
                .andExpect(jsonPath(path + ".status").value("已通过"))
                .andExpect(jsonPath(path + ".conflictFlag").value(1))
                .andExpect(jsonPath(path + ".conflict").value(true))
                .andExpect(jsonPath(path + ".reviewerName").value("张明"))
                .andExpect(jsonPath(path + ".reviewer").value("张明"))
                .andExpect(jsonPath(path + ".reviewRemark").value("门禁已同步放行。"))
                .andExpect(jsonPath(path + ".note").value("门禁已同步放行。"))
                .andExpect(jsonPath(path + ".createdAt").value("2026-04-22T16:10:00"))
                .andExpect(jsonPath(path + ".updatedAt").value("2026-04-22T17:30:00"))
                .andExpect(jsonPath(path + ".submittedAt").value("04-22 16:10"))
                .andExpect(jsonPath(path + ".reviewedAt").value("04-22 17:30"))
                .andExpect(jsonPath(path + ".labName").value("网络实验室"))
                .andExpect(jsonPath(path + ".labCode").value("LAB-009"))
                .andExpect(jsonPath(path + ".lab").value("LAB-009 网络实验室"))
                .andExpect(jsonPath(path + ".tagType").value("success"));
    }

    private ReservationView reservation() {
        return new ReservationView(
                7L, 42L, "软件工程 2 班", "软件学院", "刘老师", 9L,
                LocalDate.of(2026, 4, 23), "08:00-10:00", "课程", 68,
                "云计算平台实训课程", ReservationStatus.APPROVED, true, "张明", "门禁已同步放行。",
                LocalDateTime.of(2026, 4, 22, 16, 10), LocalDateTime.of(2026, 4, 22, 17, 30),
                "网络实验室", "LAB-009"
        );
    }
}
