package org.example.backend.config;
import org.example.backend.security.JwtService;
import org.example.backend.websocket.AiAssistantWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SocketHandshakeBehaviorTest {
    @Test void registrationAppliesOriginsAndHandshakeAuthenticatesHeaderOrSubprotocolWithoutQueryTokens() throws Exception{JwtService jwt=mock(JwtService.class);AiAssistantWebSocketHandler handler=mock(AiAssistantWebSocketHandler.class);AiAssistantWebSocketConfig config=new AiAssistantWebSocketConfig(handler,jwt," http://localhost:*, ,https://school.test ");WebSocketHandlerRegistry registry=mock(WebSocketHandlerRegistry.class);WebSocketHandlerRegistration registration=mock(WebSocketHandlerRegistration.class,RETURNS_SELF);when(registry.addHandler(handler,"/ai-assistant/ws","/ws/ai-assistant")).thenReturn(registration);config.registerWebSocketHandlers(registry);var captor=ArgumentCaptor.forClass(HandshakeInterceptor.class);verify(registration).addInterceptors(captor.capture());verify(registration).setAllowedOriginPatterns("http://localhost:*","https://school.test");HandshakeInterceptor interceptor=captor.getValue();ServerHttpRequest request=mock(ServerHttpRequest.class);ServerHttpResponse response=mock(ServerHttpResponse.class);HttpHeaders headers=new HttpHeaders();when(request.getHeaders()).thenReturn(headers);Map<String,Object> attributes=new HashMap<>();assertFalse(interceptor.beforeHandshake(request,response,handler,attributes));headers.set("Authorization","bEaReR header-token");when(jwt.validateToken("header-token")).thenReturn(new JwtService.JwtClaims("admin","labAdmin","7",123));assertTrue(interceptor.beforeHandshake(request,response,handler,attributes));assertEquals(Map.of("authUsername","admin","authRole","labAdmin","authUserId","7"),attributes);headers.remove("Authorization");headers.set("Sec-WebSocket-Protocol","smart-lab.ai.v1, smart-lab.jwt.protocol-token");when(jwt.validateToken("protocol-token")).thenReturn(new JwtService.JwtClaims("admin","labAdmin","7",123));assertTrue(interceptor.beforeHandshake(request,response,handler,new HashMap<>()));headers.set("Authorization","Bearer invalid");when(jwt.validateToken("invalid")).thenThrow(new IllegalArgumentException("bad"));assertFalse(interceptor.beforeHandshake(request,response,handler,new HashMap<>()));headers.clear();headers.set("Sec-WebSocket-Protocol","smart-lab.ai.v1");assertFalse(interceptor.beforeHandshake(request,response,handler,new HashMap<>()));interceptor.afterHandshake(request,response,handler,null);assertEquals(20*1024*1024,config.webSocketContainer().getMaxTextMessageBufferSize());assertEquals(20*1024*1024,config.webSocketContainer().getMaxBinaryMessageBufferSize());}
}
