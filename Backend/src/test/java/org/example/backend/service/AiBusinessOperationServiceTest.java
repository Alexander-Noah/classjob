package org.example.backend.service;

import com.sun.net.httpserver.HttpServer;
import org.example.backend.controller.LabController;
import org.example.backend.controller.ReservationController;
import org.example.backend.controller.UserController;
import org.example.backend.entity.LabEntity;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiBusinessOperationServiceTest {
    @Test void interruptedReadsPreserveCancellationAndDoNotReportAFalseSuccessfulResult() {
        try {
            Thread.currentThread().interrupt();
            assertTrue(assertThrows(IllegalArgumentException.class,()->service.read(7,Map.of("operationId","GET /labs/{id:\\d+}","pathValues",Map.of("id","1")),request)).getMessage().contains("请求中断"));
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(0,writes.get());
        } finally { Thread.interrupted(); }
    }
    @Test @SuppressWarnings("unchecked") void discoveryDescribesRealQueryParametersAndOmitsUploadsAndNonControllerHandlers() throws Exception {
        when(data.actor(7)).thenReturn(new AiDataActor(7,"systemAdmin","管理员",Set.of()));
        var mapping=mock(RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo,HandlerMethod> routes=new LinkedHashMap<>();
        var activation=mock(org.example.backend.controller.ActivationCodeController.class);
        routes.put(RequestMappingInfo.paths("/activation-codes").methods(RequestMethod.GET,RequestMethod.OPTIONS).build(),new HandlerMethod(activation,org.example.backend.controller.ActivationCodeController.class.getMethod("listActivationCodes",Map.class)));
        routes.put(RequestMappingInfo.paths("/activation-codes").methods(RequestMethod.POST).build(),new HandlerMethod(activation,org.example.backend.controller.ActivationCodeController.class.getMethod("createActivationCodes",Map.class)));
        var approvals=mock(org.example.backend.controller.ApprovalCountersignController.class);
        routes.put(RequestMappingInfo.paths("/approval-countersigns").methods(RequestMethod.GET).build(),new HandlerMethod(approvals,org.example.backend.controller.ApprovalCountersignController.class.getMethod("listCountersigns",String.class,String.class,String.class,String.class)));
        var devices=mock(org.example.backend.controller.DevicesController.class);
        routes.put(RequestMappingInfo.paths("/devices/import").methods(RequestMethod.POST).build(),new HandlerMethod(devices,org.example.backend.controller.DevicesController.class.getMethod("importDevices",org.springframework.web.multipart.MultipartFile.class)));
        routes.put(RequestMappingInfo.paths("/labs").methods(RequestMethod.GET).build(),new HandlerMethod("outside",String.class.getMethod("isEmpty")));
        when(mapping.getHandlerMethods()).thenReturn(routes);
        ObjectProvider<RequestMappingHandlerMapping> provider=mock(ObjectProvider.class);when(provider.getObject()).thenReturn(mapping);
        var discover=new AiBusinessOperationService(provider,data);
        var activationCatalog=discover.catalog(7,"activation-codes");
        assertEquals(2,activationCatalog.size());
        assertTrue(activationCatalog.stream().anyMatch(item->!((Map<?,?>)item.get("query")).isEmpty()));
        assertTrue(activationCatalog.stream().anyMatch(item->item.getOrDefault("body","").toString().contains("object")));
        var approvalCatalog=discover.catalog(7,"approval-countersigns");
        assertEquals(1,approvalCatalog.size());
        assertTrue(((Map<?,?>)approvalCatalog.get(0).get("query")).containsKey("businessType"));
        assertTrue(discover.catalog(7,"devices").isEmpty());assertTrue(discover.catalog(7,"labs").isEmpty());
        assertEquals(0,reads.get());assertEquals(0,writes.get());
    }
    @Test void catalogDescribesAllowedBodyFieldsAndModulesWithoutCallingBackend(){assertTrue(service.modules(7).stream().anyMatch(module->"labs".equals(module.get("module"))));List<Map<String,Object>> catalog=service.catalog(7,"labs");assertTrue(catalog.stream().anyMatch(operation->operation.containsKey("body")&&operation.get("body").toString().contains("labName")));assertEquals(0,reads.get());assertEquals(0,writes.get());}
    @Test void readForwardsRepeatedQueryValuesAndReportsMalformedBackendResponse(){Object result=service.read(7,Map.of("operationId","GET /labs/{id:\\d+}","pathValues",Map.of("id","1"),"query",Map.of("status",List.of("online","offline"))),request);assertNotNull(result);assertEquals(1,reads.get());record="not-json";assertTrue(assertThrows(IllegalArgumentException.class,()->service.read(7,Map.of("operationId","GET /labs/{id:\\d+}","pathValues",Map.of("id","1")),request)).getMessage().contains("核实"));request.removeHeader("Authorization");assertTrue(assertThrows(IllegalArgumentException.class,()->service.read(7,Map.of("operationId","GET /labs/{id:\\d+}","pathValues",Map.of("id","1")),request)).getMessage().contains("登录"));}
    private HttpServer server;
    private final AtomicInteger writes = new AtomicInteger();
    private final AtomicInteger reads = new AtomicInteger();
    private int responseStatus=200;
    private String record = "{\"code\":200,\"message\":\"查询成功\",\"data\":{\"id\":1,\"labName\":\"204\",\"password\":\"hidden\"}}";
    private AiBusinessOperationService service;
    private AiBusinessDataService data;
    private MockHttpServletRequest request;
    private final MutableClock clock = new MutableClock();

    @BeforeEach @SuppressWarnings("unchecked") void setup() throws Exception {
        data = mock(AiBusinessDataService.class);
        when(data.actor(7)).thenReturn(new AiDataActor(7, "labAdmin", "李四", Set.of()));
        when(data.actor(8)).thenReturn(new AiDataActor(8, "teacher", "教师", Set.of()));
        var mapping = mock(RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> handlers = new LinkedHashMap<>();
        LabController labs = mock(LabController.class);
        handlers.put(RequestMappingInfo.paths("/labs").methods(RequestMethod.POST).build(),
                new HandlerMethod(labs, LabController.class.getMethod("addLab", LabEntity.class)));
        handlers.put(RequestMappingInfo.paths("/labs/{id:\\d+}").methods(RequestMethod.PUT, RequestMethod.GET, RequestMethod.DELETE).build(),
                new HandlerMethod(labs, LabController.class.getMethod("updateLab", Integer.class, LabEntity.class)));
        handlers.put(RequestMappingInfo.paths("/reservations").methods(RequestMethod.POST).build(),
                new HandlerMethod(mock(ReservationController.class), ReservationController.class.getMethod("list", jakarta.servlet.http.HttpServletRequest.class)));
        handlers.put(RequestMappingInfo.paths("/auth/login").methods(RequestMethod.POST).build(),
                new HandlerMethod(mock(UserController.class), UserController.class.getMethods()[0]));
        when(mapping.getHandlerMethods()).thenReturn(handlers);
        ObjectProvider<RequestMappingHandlerMapping> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(mapping);
        service = new AiBusinessOperationService(provider, data, clock);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/labs", exchange -> {
            assertEquals("Bearer test-session", exchange.getRequestHeaders().getFirst("Authorization"));
            String response;
            if (exchange.getRequestMethod().equals("GET")) { reads.incrementAndGet(); response = record; }
            else { writes.incrementAndGet(); response = "{\"code\":200,\"message\":\"保存成功\",\"data\":{\"id\":1}}"; }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        request = new MockHttpServletRequest();
        request.setLocalPort(server.getAddress().getPort());
        request.addHeader("Authorization", "Bearer test-session");
    }
    @AfterEach void stop() { server.stop(0); }
    private Map<String, Object> action(String method) {
        return Map.of("operationId", method + " /labs/{id:\\d+}", "pathValues", Map.of("id", "1"),
                "body", Map.of("labName", "新名称"), "summary", "修改204实验室名称");
    }
    @Test void transportFailureCannotBeTreatedAsSuccessEvenWhenJsonCodeSaysSuccess() {
        responseStatus=503;
        assertThrows(IllegalArgumentException.class,()->service.read(7,Map.of("operationId","GET /labs/{id:\\d+}","pathValues",Map.of("id","1")),request));
        assertEquals(1,reads.get());assertEquals(0,writes.get());
    }
    private String prepare() { return (String) service.prepare(7, "chat-a", action("PUT"), request).get("id"); }

    @Test void preparationOnlyReadsAndConfirmationWritesExactlyOnce() {
        Map<String, Object> draft = service.prepare(7, "chat-a", action("PUT"), request);
        assertEquals(0, writes.get()); assertEquals(1, reads.get());
        assertFalse(draft.get("before").toString().contains("hidden"));
        String token = (String) draft.get("id");
        assertThrows(IllegalArgumentException.class, () -> service.decide(8, "chat-a", token, true, request));
        assertThrows(IllegalArgumentException.class, () -> service.decide(7, "chat-b", token, true, request));
        service.decide(7, "chat-a", token, true, request);
        assertEquals(1, writes.get());
        assertThrows(IllegalArgumentException.class, () -> service.decide(7, "chat-a", token, true, request));
        assertEquals(1, writes.get());
    }
    @Test void cancellationExpiryAndNewTurnNeverWrite() {
        String cancelled = prepare(); service.decide(7, "chat-a", cancelled, false, request);
        assertThrows(IllegalArgumentException.class, () -> service.decide(7, "chat-a", cancelled, true, request));
        String expired = prepare(); clock.now = clock.now.plusSeconds(601);
        assertThrows(IllegalArgumentException.class, () -> service.decide(7, "chat-a", expired, true, request));
        String superseded = prepare(); service.invalidate(7, "chat-a");
        assertThrows(IllegalArgumentException.class, () -> service.decide(7, "chat-a", superseded, true, request));
        assertEquals(0, writes.get());
    }
    @Test void changedRecordsAndRevokedRolesRequireANewConfirmation() {
        String token = prepare();
        record = "{\"code\":200,\"data\":{\"id\":1,\"labName\":\"别人修改过\"}}";
        assertThrows(IllegalArgumentException.class, () -> service.decide(7, "chat-a", token, true, request));
        String revoked = prepare();
        when(data.actor(7)).thenReturn(new AiDataActor(7, "teacher", "李四", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> service.decide(7, "chat-a", revoked, true, request));
        assertEquals(0, writes.get());
    }
    @Test void queryCannotWriteOrCallUnregisteredPathsAndRolesFilterCatalog() {
        assertThrows(IllegalArgumentException.class, () -> service.read(7, action("DELETE"), request));
        assertThrows(IllegalArgumentException.class, () -> service.prepare(8, "chat-a", action("PUT"), request));
        assertThrows(IllegalArgumentException.class, () -> service.prepare(7, "chat-a", Map.of("operationId", "POST /auth/login"), request));
        assertTrue(service.catalog(8, "labs").isEmpty());
        assertFalse(service.catalog(7, "reservations").isEmpty());
        assertEquals(0, writes.get());
    }
    @Test void pathTraversalAndCredentialsAreRejectedBeforeDispatch() {
        Map<String, Object> unsafe = new HashMap<>(action("PUT"));
        unsafe.put("pathValues", Map.of("id", "../users/1"));
        assertThrows(IllegalArgumentException.class, () -> service.prepare(7, "chat-a", unsafe, request));
        unsafe.put("pathValues", Map.of("id", "1")); unsafe.put("body", Map.of("nested", Map.of("apiKey", "secret")));
        assertThrows(IllegalArgumentException.class, () -> service.prepare(7, "chat-a", unsafe, request));
        assertEquals(0, reads.get()); assertEquals(0, writes.get());
    }
    @Test void menuPermissionsAreCheckedAgainAtConfirmation() {
        String token = prepare();
        when(data.actor(7)).thenReturn(new AiDataActor(7, "labAdmin", "李四", Set.of("AI智能助手", "预约管理")));
        assertTrue(service.catalog(7, "labs").isEmpty());
        assertFalse(service.catalog(7, "reservations").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.decide(7, "chat-a", token, true, request));
        assertEquals(0, writes.get());
    }
    @Test void creationNeedsConfirmationWithoutReadingAnUnrelatedRecord() {
        var draft=service.prepare(7,"new-lab",Map.of("operationId","POST /labs","body",Map.of("labName","新增实验室","tags",List.of("教学","开放")),"query",Map.of("source","assistant")),request);
        assertFalse(draft.containsKey("before"));assertEquals(0,reads.get());assertEquals(0,writes.get());
        assertNotNull(service.decide(7,"new-lab",draft.get("id").toString(),true,request));assertEquals(1,writes.get());
        var deletion=service.prepare(7,"delete-lab",action("DELETE"),request);
        assertTrue(deletion.get("warning").toString().contains("关联设备"));
    }
    @Test void malformedObjectsOversizedBodiesAndNestedSecretQueriesNeverDispatch() {
        assertThrows(IllegalArgumentException.class,()->service.prepare(7,"chat",Map.of("operationId","GET /labs/{id:\\d+}"),request));
        assertThrows(IllegalArgumentException.class,()->service.prepare(7,"chat",Map.of("operationId","POST /labs","body",List.of("invalid")),request));
        assertThrows(IllegalArgumentException.class,()->service.prepare(7,"chat",Map.of("operationId","POST /labs","body",Map.of("remark","a".repeat(24001))),request));
        assertThrows(IllegalArgumentException.class,()->service.prepare(7,"chat",Map.of("operationId","POST /labs","query",Map.of("filters",List.of(Map.of("password","secret")))),request));
        assertEquals(0,reads.get());assertEquals(0,writes.get());
    }
    @Test void invalidatingOneConversationPreservesOtherActorsAndConversations() {
        String a=prepare();
        String b=service.prepare(7,"chat-b",action("PUT"),request).get("id").toString();
        when(data.actor(9)).thenReturn(new AiDataActor(9,"labAdmin","其他管理员",Set.of()));
        String c=service.prepare(9,"chat-a",action("PUT"),request).get("id").toString();
        service.invalidate(7,"chat-a");
        assertThrows(IllegalArgumentException.class,()->service.decide(7,"chat-a",a,true,request));
        service.decide(7,"chat-b",b,false,request);service.decide(9,"chat-a",c,false,request);
        assertEquals(0,writes.get());
        String expired=prepare();clock.now=clock.now.plusSeconds(601);service.invalidate(9,"unrelated");
        assertThrows(IllegalArgumentException.class,()->service.decide(7,"chat-a",expired,true,request));
    }
    @Test void businessErrorsAreReportedAndResponsesRedactSecretsInsideLists() {
        var query=Map.<String,Object>of("operationId","GET /labs/{id:\\d+}","pathValues",Map.of("id","1"),"query",Map.of("page",1));
        record="{\"code\":400,\"message\":\"实验室不可用\"}";
        assertEquals("实验室不可用",assertThrows(IllegalArgumentException.class,()->service.read(7,query,request)).getMessage());
        record="{\"data\":[{\"labName\":\"A101\",\"password\":\"hidden\"}]}";
        Object result=service.read(7,query,request);assertTrue(result.toString().contains("A101"));assertFalse(result.toString().contains("hidden"));
        request.removeHeader("Authorization");request.addHeader("Authorization","Basic invalid");
        assertThrows(IllegalArgumentException.class,()->service.read(7,query,request));
    }
    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-18T06:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
}
