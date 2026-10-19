package org.example.backend.controller;

import org.example.backend.entity.*;
import org.example.backend.service.*;
import org.example.backend.security.AuthenticationAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AcademicEndpointBehaviorTest {
    @Test void academicActionsRouteRequestsAndCredentialKeys() throws Exception {
        AcademicScheduleService schedules=mock(AcademicScheduleService.class);
        AcademicCredentialService credentials=mock(AcademicCredentialService.class);
        MockMvc mvc=MockMvcBuilders.standaloneSetup(new AcademicScheduleController(schedules,credentials)).build();
        for(String action:List.of("parse","fetch","import"))mvc.perform(post("/academic-schedules/"+action).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(schedules).parseSchedule(any());verify(schedules).fetchSchedule(any());verify(schedules).importSchedule(any());
        mvc.perform(get("/academic-schedules/credentials/test")).andExpect(status().isOk());
        mvc.perform(post("/academic-schedules/credentials").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
        mvc.perform(delete("/academic-schedules/credentials/test")).andExpect(jsonPath("$.data").value(true));
        verify(credentials).getCredentialView("test");verify(credentials).saveCredential(any());verify(credentials).deleteCredential("test");
    }
    @Test void crawlerEndpointsForwardLimitsAndConfigurationWithoutStartingProcesses() throws Exception {
        ClassTimetableService service=mock(ClassTimetableService.class);MockMvc mvc=MockMvcBuilders.standaloneSetup(new ClassTimetableController(service)).build();
        mvc.perform(post("/class-timetables/crawl")).andExpect(status().isOk());
        mvc.perform(get("/class-timetables/crawler-config")).andExpect(status().isOk());
        mvc.perform(post("/class-timetables/crawler-config").contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}")).andExpect(status().isOk());
        mvc.perform(get("/class-timetables/crawler-tasks").param("limit","5")).andExpect(status().isOk());
        mvc.perform(delete("/class-timetables/crawler-tasks")).andExpect(jsonPath("$.data").value(true));
        mvc.perform(get("/class-timetables/credential")).andExpect(status().isOk());
        mvc.perform(post("/class-timetables/credential").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"test\"}")).andExpect(status().isOk());
        mvc.perform(delete("/class-timetables/credential")).andExpect(jsonPath("$.data").value(true));
        verify(service).triggerCrawler();verify(service).getCrawlerConfig();verify(service).saveCrawlerConfig(Map.of("enabled",true));verify(service).listCrawlerTasks(5);verify(service).clearCrawlerTasks();verify(service).getCrawlerCredential();verify(service).saveCrawlerCredential(Map.of("username","test"));verify(service).deleteCrawlerCredential();
    }
    @Test void hostDiscoveryHidesConnectionTokenAndSupportsOfflineResponse() throws Exception {
        TeacherHostService service=mock(TeacherHostService.class);MockMvc mvc=MockMvcBuilders.standaloneSetup(new TeacherHostController(service)).build();
        TeacherHostEntity host=new TeacherHostEntity();host.setId(1L);host.setLabId(2L);host.setTeacherDeviceId("pc");host.setHostIp("127.0.0.1");host.setPort(8000);host.setStatus("online");host.setToken("private");when(service.currentHost(2L)).thenReturn(host);
        mvc.perform(get("/teacher-hosts/current").param("labId","2")).andExpect(jsonPath("$.data.hostIp").value("127.0.0.1")).andExpect(jsonPath("$.data.token").doesNotExist());
        mvc.perform(get("/api/teacher-hosts/current").param("labId","3")).andExpect(jsonPath("$.data").doesNotExist()).andExpect(jsonPath("$.message").value("current lab teacher host is offline"));
        when(service.heartbeat(any())).thenAnswer(c->c.getArgument(0));mvc.perform(post("/api/teacher-hosts/heartbeat").contentType(MediaType.APPLICATION_JSON).content("{\"labId\":2}")).andExpect(jsonPath("$.data.labId").value(2));verify(service).heartbeat(argThat(h->h.getLabId()==2));
    }
    @Test void courseEnvironmentCrudForwardsNumericIdentifiers() throws Exception {
        CourseEnvironmentService service=mock(CourseEnvironmentService.class);CourseEnvironmentController controller=new CourseEnvironmentController();ReflectionTestUtils.setField(controller,"courseEnvironmentService",service);MockMvc mvc=MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/course-environment-requests")).andExpect(status().isOk());mvc.perform(get("/course-environment-requests/7")).andExpect(status().isOk());
        mvc.perform(post("/course-environment-requests").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());mvc.perform(put("/course-environment-requests/7").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());mvc.perform(delete("/course-environment-requests/7")).andExpect(status().isOk());mvc.perform(post("/course-environment-requests/7/confirm")).andExpect(status().isOk());
        verify(service).getEnvironment();verify(service).getCourseEnvironment(7);verify(service).InserterCourseEnvironment(any());verify(service).updateCourseEnvironment(eq(7),any());verify(service).deleteCourseEnvironment(7);verify(service).confirmCourseEnvironment(7);
    }
    @Test void maintenanceMutationsRequireActiveWriterWhileReadsRemainAvailable() throws Exception {
        MaintenanceRecordService service=mock(MaintenanceRecordService.class);UserService users=mock(UserService.class);MockMvc mvc=MockMvcBuilders.standaloneSetup(new MaintenanceRecordController(service,users)).build();
        mvc.perform(get("/maintenance-records")).andExpect(status().isOk());mvc.perform(get("/maintenance-records/R1")).andExpect(status().isOk());verify(service).listMaintenanceRecords();verify(service).getMaintenanceRecord("R1");
        mvc.perform(post("/maintenance-records").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/maintenance-records/R1").requestAttr(AuthenticationAttributes.USERNAME,"test")).andExpect(status().isForbidden());
        UserEntity user=new UserEntity();user.setStatus(0);user.setRoleCode("systemAdmin");when(users.profile("test")).thenReturn(user);
        mvc.perform(delete("/maintenance-records/R1").requestAttr(AuthenticationAttributes.USERNAME,"test")).andExpect(status().isForbidden());
        user.setStatus(1);user.setRoleCode("student");mvc.perform(delete("/maintenance-records/R1").requestAttr(AuthenticationAttributes.USERNAME,"test")).andExpect(status().isForbidden());
        user.setRoleCode("systemAdmin");mvc.perform(post("/maintenance-records").requestAttr(AuthenticationAttributes.USERNAME,"test").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());mvc.perform(put("/maintenance-records/R1").requestAttr(AuthenticationAttributes.USERNAME,"test").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());mvc.perform(delete("/maintenance-records/R1").requestAttr(AuthenticationAttributes.USERNAME,"test")).andExpect(status().isOk());verify(service).createMaintenanceRecord(any());verify(service).updateMaintenanceRecord(eq("R1"),any());verify(service).deleteMaintenanceRecord("R1");
    }
}
