package org.example.backend.service;

import org.example.backend.config.IotHardwareProperties;
import org.example.backend.config.IotHardwareProperties.HardwareDevice;
import org.example.backend.config.IotHardwareProperties.HardwareCommand;
import org.example.backend.service.impl.IotHardwareServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IotHardwareBehaviorTest {
    @Test void localizedAccessActionAliasesResolveToConfiguredCommandKeys() throws Exception{var door=device("door","门禁");Map<String,HardwareCommand> commands=new java.util.HashMap<>();for(String key:List.of("card-enroll","student-enroll","face-enroll","fingerprint-enroll","status","read-environment","lock"))commands.put(key,command("POST","/"+key));door.setCommands(commands);properties.setDevices(List.of(door));ReflectionTestUtils.setField(service,"httpClient",client);when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(call->response(200,"OK"));for(String action:List.of("卡录入","学号录入","人脸录入","指纹录入","电子锁","温湿度","关门"))assertEquals(true,service.executeDeviceCommand("door",action,Map.of()).get("success"));verify(client,times(7)).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));}
    private final IotHardwareProperties properties = new IotHardwareProperties();
    private final BusinessLoopService events = mock(BusinessLoopService.class);
    private final IotHardwareServiceImpl service = new IotHardwareServiceImpl(properties, events);
    private final HttpClient client = mock(HttpClient.class);

    @Test void overviewSanitizesCredentialsNormalizesDeviceTypesAndReturnsCameraProxy() {
        var door = device("door", "门禁"); door.setUsername("admin"); door.setPassword("test-secret"); door.setToken("test-token");
        var camera = device("camera", "监控摄像头"); camera.setSnapshotUrl("/snapshot"); camera.setHlsUrl("/stream.m3u8");
        properties.setDevices(List.of(door,camera));
        var overview = service.getOverview(); assertEquals(1L,overview.get("accessCount")); assertEquals(1L,overview.get("cameraCount"));
        var sanitized = service.listHardwareDevices().get(0); assertFalse(sanitized.containsKey("password")); assertFalse(sanitized.containsKey("token")); assertEquals("access-control",sanitized.get("type"));
        assertEquals("/iot/cameras/camera/snapshot", service.getLabCamera(7L).get("snapshotProxyUrl"));
        assertEquals("/stream.m3u8", service.getLabCamera(7L).get("streamUrl"));
        assertEquals(true, service.getLabStatus(7L).get("online"));
        assertEquals("已配置",service.getDeviceStatus("door").get("status"));
        assertThrows(IllegalArgumentException.class, () -> service.getLabStatus(999L));
        assertThrows(IllegalArgumentException.class, () -> service.getDeviceStatus("missing"));
        assertThrows(IllegalArgumentException.class, () -> service.executeAccessCommand(999L,"open",Map.of()));
        properties.setEnabled(false); assertThrows(IllegalArgumentException.class, () -> service.getLabCamera(7L));
    }

    @Test void configuredHttpCommandsUseNormalizedActionsTemplatedPathsAndAuthentication() throws Exception {
        var door = device("door","门禁"); door.setToken("test-token"); door.setHeaders(Map.of("X-Device","configured"));
        var command = command("PUT","/device/{deviceCode}"); command.setBody("{\"lab\":\"{labId}\",\"value\":\"{value}\"}"); command.setHeaders(Map.of("X-Command","set")); command.setSuccessContains("OK");
        door.setCommands(Map.of("open",command)); properties.setDevices(List.of(door));
        ReflectionTestUtils.setField(service,"httpClient",client);
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(call -> response(200,"OK"));
        assertEquals(true,service.executeAccessCommand(7L,"远程开门",Map.of("value",10)).get("success"));
        var captor=org.mockito.ArgumentCaptor.forClass(HttpRequest.class); verify(client).send(captor.capture(),any(HttpResponse.BodyHandler.class));
        assertEquals("/device/door",captor.getValue().uri().getPath()); assertEquals("PUT",captor.getValue().method());
        assertEquals("Bearer test-token",captor.getValue().headers().firstValue("Authorization").orElseThrow());
        assertEquals("configured",captor.getValue().headers().firstValue("X-Device").orElseThrow());
        assertEquals("set",captor.getValue().headers().firstValue("X-Command").orElseThrow());
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(call -> response(200,"rejected"));
        assertEquals(false,service.executeDeviceCommand("door","unlock",null).get("success"));
        verify(events).recordEvent(eq("iot"),eq("access-open"),eq("door"),eq("执行成功"),anyMap());
        assertThrows(IllegalArgumentException.class, () -> service.executeDeviceCommand("door","unknown",Map.of()));
    }

    @Test void httpStatusAndSnapshotRespectResponseBytesAndHeadersAndHandleFailure() throws Exception {
        var camera=device("camera","camera"); camera.setSnapshotUrl("https://example.test/snapshot"); camera.setStatusPath("/status"); camera.setUsername("admin"); camera.setPassword("test-password"); properties.setDevices(List.of(camera));
        ReflectionTestUtils.setField(service,"httpClient",client);
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(call -> response(200,"image-data"));
        assertEquals(true,service.getDeviceStatus("camera").get("online"));
        var snapshot=service.getCameraSnapshot("camera"); assertArrayEquals("image-data".getBytes(StandardCharsets.UTF_8),snapshot.bytes()); assertEquals("image/png",snapshot.contentType());
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(call -> response(500,"failed"));
        assertThrows(IllegalArgumentException.class, () -> service.getCameraSnapshot("camera"));
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("offline"));
        assertThrows(IllegalArgumentException.class, () -> service.getDeviceStatus("camera"));
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenThrow(new InterruptedException());
        try { assertThrows(IllegalArgumentException.class, () -> service.getDeviceStatus("camera")); assertTrue(Thread.currentThread().isInterrupted()); } finally { Thread.interrupted(); }
        camera.setSnapshotUrl(""); assertThrows(IllegalArgumentException.class, () -> service.getCameraSnapshot("camera"));
    }

    @Test void tcpCommandsSendHexBytesAndInterpretAcknowledgementsWithoutExternalNetwork() throws Exception {
        var door=device("door","door"); door.setProtocol("tcp"); var command=command("POST","/unused"); command.getTcp().setHost("127.0.0.1"); command.getTcp().setPort(12345); command.getTcp().setPayload("hex:AA 01 FF"); command.getTcp().setSuccessContains("ACK"); door.setCommands(Map.of("open",command)); properties.setDevices(List.of(door));
        var outbound=new ByteArrayOutputStream();
        try(var sockets=mockConstruction(Socket.class,(socket,context)->{ when(socket.getOutputStream()).thenReturn(outbound); when(socket.getInputStream()).thenReturn(new ByteArrayInputStream("ACK".getBytes(StandardCharsets.UTF_8))); })) {
            assertEquals(true,service.executeDeviceCommand("door","open",Map.of()).get("success")); assertArrayEquals(new byte[]{(byte)0xAA,1,(byte)0xFF},outbound.toByteArray()); verify(sockets.constructed().get(0)).close();
        }
        command.getTcp().setPort(null); assertThrows(IllegalArgumentException.class,()->service.executeDeviceCommand("door","open",Map.of()));
    }

    @Test void missingUrlsAndBaseUrlsRejectBeforeHttpAndTypeAliasesRemainStable() {
        var door=device("door","door"); door.setCommands(Map.of("open",new HardwareCommand())); properties.setDevices(List.of(door));
        assertThrows(IllegalArgumentException.class,()->service.executeDeviceCommand("door","open",Map.of()));
        door.setCommands(Map.of("open",command("POST","/open"))); door.setBaseUrl(null); assertThrows(IllegalArgumentException.class,()->service.executeDeviceCommand("door","open",Map.of()));
        for(String[] pair:new String[][]{{"电子锁","access-control"},{"监控","camera"},{"照明","light"},{"温控","air-conditioner"},{"温湿度","sensor"},{"消防","safety"},{"custom","custom"}}) {
            door.setType(pair[0]); assertEquals(pair[1],service.listHardwareDevices().get(0).get("type"));
        }
    }
    @Test void configuredStatusAndFallbackStatusUseTheirRespectiveResponseContracts() throws Exception {
        var hardware=device("door","door"); properties.setDevices(List.of(hardware));
        ReflectionTestUtils.setField(service,"httpClient",client);
        assertEquals("已配置",service.executeDeviceCommand("door","status",null).get("status"));
        hardware.setCommands(Map.of("status",command("GET","/configured-status")));
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(call->response(200,"ready"));
        assertEquals(true,service.getDeviceStatus("door").get("success"));
        hardware.setCommands(Map.of()); hardware.setStatusPath("/status");
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(call->response(503,"offline"));
        assertEquals(false,service.getDeviceStatus("door").get("online"));
        assertEquals("离线",service.getDeviceStatus("door").get("status"));
        hardware.setType("camera"); assertFalse(service.getLabCamera(7L).containsKey("snapshotProxyUrl"));
    }

    @Test void customSuccessStatusRangeAndDeleteRequestsHandleLowAndHighFailures() throws Exception {
        var hardware=device("relay","relay"); hardware.setLabId(null); hardware.setUsername("admin");
        var command=command("DELETE","http://example.test/delete"); command.setSuccessStatusMin(201);command.setSuccessStatusMax(202);
        hardware.setCommands(Map.of("off",command)); properties.setDevices(List.of(hardware));
        ReflectionTestUtils.setField(service,"httpClient",client);
        for(int status:List.of(200,201,202,203)) {
            when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(call->response(status,"OK"));
            assertEquals(status==201||status==202,service.executeDeviceCommand("relay","off",Map.of()).get("success"));
        }
        var requests=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(client,times(4)).send(requests.capture(),any(HttpResponse.BodyHandler.class));
        assertTrue(requests.getAllValues().stream().allMatch(request->"DELETE".equals(request.method())&&request.bodyPublisher().map(body->body.contentLength()==0).orElse(true)));
        assertTrue(requests.getAllValues().stream().allMatch(request->request.headers().firstValue("Authorization").isEmpty()));
    }

    @Test void tcpTextEmptyPayloadChunkedResponsesAndTimeoutAcknowledgementsAreHandled() throws Exception {
        var hardware=device("relay","relay");hardware.setProtocol("tcp");
        var command=command("POST","/unused");command.getTcp().setHost("127.0.0.1");command.getTcp().setPort(12345);
        hardware.setCommands(Map.of("off",command));properties.setDevices(List.of(hardware));
        for(String payload:List.of("", "关灯")) {
            command.getTcp().setPayload(payload);command.getTcp().setSuccessContains(null);
            var outgoing=new ByteArrayOutputStream();
            var incoming=mock(java.io.InputStream.class);
            when(incoming.read(any(byte[].class))).thenThrow(new java.net.SocketTimeoutException());
            try(var sockets=mockConstruction(Socket.class,(socket,context)->{when(socket.getOutputStream()).thenReturn(outgoing);when(socket.getInputStream()).thenReturn(incoming);})) {
                assertEquals(true,service.executeDeviceCommand("relay","off",null).get("success"));
                assertArrayEquals(payload.getBytes(StandardCharsets.UTF_8),outgoing.toByteArray());
            }
        }
        command.getTcp().setPayload("stop");command.getTcp().setSuccessContains("ACK");
        var incoming=mock(java.io.InputStream.class);when(incoming.available()).thenReturn(1,0);
        when(incoming.read(any(byte[].class))).thenAnswer(call->{((byte[])call.getArgument(0))[0]='N';return 1;}).thenReturn(-1);
        try(var sockets=mockConstruction(Socket.class,(socket,context)->{when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());when(socket.getInputStream()).thenReturn(incoming);})) {
            assertEquals(false,service.executeDeviceCommand("relay","off",Map.of()).get("success"));
        }
        try(var sockets=mockConstruction(Socket.class,(socket,context)->{doThrow(new IOException("offline")).when(socket).connect(any(),anyInt());})) {
            assertTrue(assertThrows(IllegalArgumentException.class,()->service.executeDeviceCommand("relay","off",Map.of())).getMessage().contains("offline"));
        }
        hardware.setBaseUrl("");command.getTcp().setHost("");
        assertThrows(IllegalArgumentException.class,()->service.executeDeviceCommand("relay","off",Map.of()));
    }

    @Test void allDeviceTypeAliasesAreNormalizedInPublicInventory() {
        var hardware=device("hardware","door");properties.setDevices(List.of(hardware));
        for(String[] pair:new String[][]{{"门锁","access-control"},{"灯","light"},{"relay","light"},{"空调","air-conditioner"},{"hvac","air-conditioner"},{"传感","sensor"},{"烟感","sensor"},{"安防","safety"}}) {
            hardware.setType(pair[0]);assertEquals(pair[1],service.listHardwareDevices().get(0).get("type"));
        }
    }

    private HardwareDevice device(String code,String type) { var value=new HardwareDevice(); value.setCode(code); value.setName(code); value.setType(type); value.setLabId(7L); value.setBaseUrl("https://example.test"); return value; }
    @Test void descriptiveHardwareTypeNamesResolveLikeTheirCanonicalTypes() {
        var hardware=device("hardware","door");properties.setDevices(List.of(hardware));
        for(String[] pair:new String[][]{{"楼宇门禁控制器","access-control"},{"网络监控终端","camera"},{"智能照明系统","light"},{"network-relay-switch","light"},{"中央空调控制器","air-conditioner"},{"房间温控器","air-conditioner"},{"hvac-controller","air-conditioner"},{"智能烟感探头","sensor"},{"室内温湿度计","sensor"},{"楼宇安防系统","safety"},{"消防报警器","safety"}}) {
            hardware.setType(pair[0]);assertEquals(pair[1],service.listHardwareDevices().get(0).get("type"));
        }
    }
    private HardwareCommand command(String method,String path) { var value=new HardwareCommand(); value.setMethod(method); value.setPath(path); return value; }
    @SuppressWarnings("unchecked") private HttpResponse<byte[]> response(int status,String body) { HttpResponse<byte[]> value=mock(HttpResponse.class); when(value.statusCode()).thenReturn(status); when(value.body()).thenReturn(body.getBytes(StandardCharsets.UTF_8)); when(value.headers()).thenReturn(java.net.http.HttpHeaders.of(Map.of("Content-Type",List.of("image/png")),(a,b)->true)); return value; }
}
