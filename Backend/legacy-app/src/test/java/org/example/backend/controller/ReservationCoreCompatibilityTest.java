package org.example.backend.controller;

import org.example.backend.entity.ReservationsEntity;
import org.example.backend.service.ReservationsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationCoreCompatibilityTest {
    private ReservationsService reservationsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reservationsService = mock(ReservationsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ReservationsController(reservationsService)).build();
    }

    @Test
    void preservesListContractAndEveryFrontendReservationField() throws Exception {
        when(reservationsService.getReservations()).thenReturn(List.of(reservation()));

        assertReservationFields(mockMvc.perform(get("/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("获取预约列表成功")), "$.data[0]");
    }

    @Test
    void preservesDetailContractAndEveryFrontendReservationField() throws Exception {
        when(reservationsService.getReservation(7)).thenReturn(reservation());

        assertReservationFields(mockMvc.perform(get("/reservations/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("获取预约详情成功")), "$.data");
    }

    @Test
    void preservesMissingDetailAsSuccessfulNullResponse() throws Exception {
        when(reservationsService.getReservation(999)).thenReturn(null);

        mockMvc.perform(get("/reservations/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("获取预约详情成功"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void preservesCreateContractAndPersistedCompatibleResponseBody() throws Exception {
        assertReservationFields(mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 7,
                                  "applicantUserId": 42,
                                  "applicantName": "软件工程 2 班",
                                  "department": "软件学院",
                                  "contact": "刘老师",
                                  "labId": 9,
                                  "reservationDate": "2026-04-23",
                                  "timeRange": "08:00-10:00",
                                  "scene": "课程",
                                  "attendeeCount": 68,
                                  "reason": "云计算平台实训课程",
                                  "status": "已通过",
                                  "conflictFlag": 1,
                                  "reviewerName": "张明",
                                  "reviewRemark": "门禁已同步放行。",
                                  "createdAt": "2026-04-22T16:10:00",
                                  "updatedAt": "2026-04-22T17:30:00",
                                  "labName": "网络实验室",
                                  "labCode": "LAB-009"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("新增预约成功")), "$.data");

        verify(reservationsService).insertReservation(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getId().equals(7L)
                        && saved.getApplicantUserId().equals(42L)
                        && saved.getReservationDate().equals(LocalDate.of(2026, 4, 23))
                        && saved.getCreatedAt().equals(LocalDateTime.of(2026, 4, 22, 16, 10))
                        && saved.getUpdatedAt().equals(LocalDateTime.of(2026, 4, 22, 17, 30))));
    }

    @Test
    void preservesApproveContract() throws Exception {
        when(reservationsService.getApproved(eq(7), any(ReservationsEntity.class))).thenReturn(reservation());

        mockMvc.perform(post("/reservations/7/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("通过预约成功"))
                .andExpect(jsonPath("$.data.id").value(7));
    }

    @Test
    void preservesRejectContract() throws Exception {
        when(reservationsService.getRejected(eq(7), any(ReservationsEntity.class))).thenReturn(reservation());

        mockMvc.perform(post("/reservations/7/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("驳回预约成功"))
                .andExpect(jsonPath("$.data.id").value(7));
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

    private ReservationsEntity reservation() {
        ReservationsEntity reservation = new ReservationsEntity();
        reservation.setId(7L);
        reservation.setApplicantUserId(42L);
        reservation.setApplicantName("软件工程 2 班");
        reservation.setDepartment("软件学院");
        reservation.setContact("刘老师");
        reservation.setLabId(9L);
        reservation.setReservationDate(LocalDate.of(2026, 4, 23));
        reservation.setTimeRange("08:00-10:00");
        reservation.setScene("课程");
        reservation.setAttendeeCount(68);
        reservation.setReason("云计算平台实训课程");
        reservation.setStatus("已通过");
        reservation.setConflictFlag(1);
        reservation.setReviewerName("张明");
        reservation.setReviewRemark("门禁已同步放行。");
        reservation.setCreatedAt(LocalDateTime.of(2026, 4, 22, 16, 10));
        reservation.setUpdatedAt(LocalDateTime.of(2026, 4, 22, 17, 30));
        reservation.setLabName("网络实验室");
        reservation.setLabCode("LAB-009");
        return reservation;
    }
}
