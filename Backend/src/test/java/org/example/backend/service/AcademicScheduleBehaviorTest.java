package org.example.backend.service;

import org.example.backend.entity.AcademicScheduleImportRequest;
import org.example.backend.entity.CourseEnvironmentEntity;
import org.example.backend.service.impl.AcademicScheduleServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AcademicScheduleBehaviorTest {
    private final CourseEnvironmentService environments = mock(CourseEnvironmentService.class);
    private final AcademicCredentialService credentials = mock(AcademicCredentialService.class);
    private final AcademicScheduleServiceImpl service = new AcademicScheduleServiceImpl(environments, credentials);
    private static final String HTML = "<table><tr><th>标题</th></tr><tr><td>无效时段</td><td>忽略</td></tr><tr><td>第1节<br>08:30~10:00</td><td>课程：程序设计<br>教师：张老师<br>班级：软件一班<br>教室：A101实验室<hr>课程：数据库<br>教师：李老师</td><td>&nbsp;</td></tr></table>";

    @Test
    void parsesPeriodAndCourseMetadataAndIgnoresHeadersAndEmptyCells() {
        var request = request(); request.setHtml(HTML); request.setWeekLabel("第3周");
        var courses = service.parseSchedule(request);
        assertEquals(2, courses.size());
        var first = courses.get(0);
        assertEquals("程序设计", first.getCourseName()); assertEquals("张老师", first.getTeacherName());
        assertEquals("软件一班", first.getClassName()); assertEquals("A101实验室", first.getLabName());
        assertEquals("周一", first.getDayLabel()); assertEquals("08:30-10:00", first.getTimeRange());
        assertEquals("第3周 周一 第1节 08:30-10:00", first.getUseTime());
        assertEquals("数据库", courses.get(1).getCourseName());
        assertThrows(IllegalArgumentException.class, () -> service.parseSchedule(null));
        request.setHtml("<table><tr><td>第2节 10:20—11:50</td><td>x</td></tr></table>");
        assertTrue(service.parseSchedule(request).isEmpty());
    }

    @Test
    void importBuildsEnvironmentContractAndOnlyReturnsPersistedEntities() {
        var request = request(); request.setHtml(HTML); request.setTeacherUserId(7);
        when(environments.InserterCourseEnvironment(any())).thenAnswer(call -> call.getArgument(0));
        var imported = service.importSchedule(request);
        assertEquals(2, imported.size());
        var first = imported.get(0);
        assertEquals("程序设计", first.getCourseName()); assertEquals(7, first.getTeacherUserId());
        assertEquals("待配置", first.getProcessStatus()); assertEquals("待确认", first.getConfirmStatus());
        assertEquals("A101实验室", first.getAssignedLabName()); assertTrue(first.getSpecialRequirements().contains("原始信息"));
        when(environments.InserterCourseEnvironment(any())).thenReturn(null);
        assertTrue(service.importSchedule(request).isEmpty());
    }

    @Test
    void fetchUsesCookieAndConfiguredPathAndParsesResponseWithoutNetwork() throws Exception {
        HttpClient client = mock(HttpClient.class);
        ReflectionTestUtils.setField(service, "httpClient", client);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response(200, HTML));
        var request = request(); request.setCookie("session=test"); request.setSchedulePath("custom/schedule");
        assertEquals(2, service.fetchSchedule(request).size());
        var captor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("/custom/schedule", captor.getValue().uri().getPath());
        assertEquals("session=test", captor.getValue().headers().firstValue("Cookie").orElseThrow());
        assertEquals(HTML, request.getHtml());
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response(500, "error"));
        assertThrows(IllegalStateException.class, () -> service.fetchSchedule(request));
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("offline"));
        assertThrows(IllegalStateException.class, () -> service.fetchSchedule(request));
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new InterruptedException());
        try { assertThrows(IllegalStateException.class, () -> service.fetchSchedule(request)); assertTrue(Thread.currentThread().isInterrupted()); }
        finally { Thread.interrupted(); }
    }

    @Test
    void fetchRejectsMissingCredentialsAndNonAcademicDomainsBeforeSending() {
        assertThrows(IllegalArgumentException.class, () -> service.fetchSchedule(null));
        for (String base : new String[]{"https://example.com", "file:///tmp/schedule"}) {
            var request = request(); request.setBaseUrl(base); request.setCookie("session=test");
            assertThrows(IllegalArgumentException.class, () -> service.fetchSchedule(request));
            request.setCookie(null); request.setUsername("user"); request.setPassword("pass");
            assertThrows(IllegalArgumentException.class, () -> service.fetchSchedule(request));
        }
    }

    @Test
    void savedCookieCredentialsAreAppliedBeforeFetching() throws Exception {
        HttpClient client = mock(HttpClient.class); ReflectionTestUtils.setField(service, "httpClient", client);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response(200, HTML));
        doAnswer(call -> { AcademicScheduleImportRequest value = call.getArgument(0); value.setCookie("saved=session"); return null; }).when(credentials).applySavedCredential(any());
        var request = request(); request.setUseSavedCredential(true);
        assertEquals(2, service.fetchSchedule(request).size()); verify(credentials).applySavedCredential(request);
    }

    @Test
    void credentialLoginObtainsCookiesAndPersistsOnlyWhenRequested() throws Exception {
        HttpClient client = mock(HttpClient.class); ReflectionTestUtils.setField(service, "httpClient", client);
        HttpClient.Builder builder = mock(HttpClient.Builder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(client);
        doAnswer(call -> {
            CookieManager manager = call.getArgument(0);
            manager.getCookieStore().add(URI.create("https://jw.hniu.cn"), new HttpCookie("session", "test"));
            manager.getCookieStore().add(URI.create("https://jw.hniu.cn"), new HttpCookie("route", "node"));
            return builder;
        }).when(builder).cookieHandler(any());
        try (var staticClient = mockStatic(HttpClient.class)) {
            staticClient.when(HttpClient::newBuilder).thenReturn(builder);
            when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(
                    response(200, "login page"), response(200, "{\"data\":\"salt#12x\"}"), response(200, "welcome"), response(200, HTML));
            var request = request(); request.setUsername("user"); request.setPassword("password"); request.setRandomCode("1234"); request.setSaveCredential(true);
            assertEquals(2, service.fetchSchedule(request).size());
            assertTrue(request.getCookie().contains("session=test")); assertTrue(request.getCookie().contains("route=node"));
            verify(credentials).saveCredential(request);
            var sent = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
            verify(client, times(4)).send(sent.capture(), any(HttpResponse.BodyHandler.class));
            assertEquals("POST", sent.getAllValues().get(2).method());
            assertEquals("application/x-www-form-urlencoded", sent.getAllValues().get(2).headers().firstValue("Content-Type").orElseThrow());
        }
    }

    @Test
    void loginErrorsDoNotFetchSchedulesOrPersistCredentials() throws Exception {
        for (String body : new String[]{"验证码错误", "密码错误", "用户名 登录"}) {
            HttpClient client = mock(HttpClient.class); HttpClient.Builder builder = mock(HttpClient.Builder.class, RETURNS_SELF);
            when(builder.build()).thenReturn(client);
            when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response(200, "page"), response(200, "{}"), response(200, body));
            try (var staticClient = mockStatic(HttpClient.class)) {
                staticClient.when(HttpClient::newBuilder).thenReturn(builder);
                var request = request(); request.setUsername("user"); request.setPassword("pass");
                assertThrows(IllegalArgumentException.class, () -> service.fetchSchedule(request));
                verify(client, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
            }
        }
        verify(credentials, never()).saveCredential(any());
    }

    private AcademicScheduleImportRequest request() { var value = new AcademicScheduleImportRequest(); value.setBaseUrl("https://jw.hniu.cn"); return value; }
    private HttpResponse<String> response(int status, String body) {
        return new HttpResponse<>() {
            public int statusCode() { return status; }
            public String body() { return body; }
            public HttpRequest request() { return HttpRequest.newBuilder(URI.create("https://jw.hniu.cn")).build(); }
            public java.util.Optional<HttpResponse<String>> previousResponse() { return java.util.Optional.empty(); }
            public java.net.http.HttpHeaders headers() { return java.net.http.HttpHeaders.of(java.util.Map.of(), (a,b) -> true); }
            public java.util.Optional<javax.net.ssl.SSLSession> sslSession() { return java.util.Optional.empty(); }
            public URI uri() { return URI.create("https://jw.hniu.cn"); }
            public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
        };
    }
}
