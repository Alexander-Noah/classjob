package org.example.backend.config;

import org.example.backend.service.*;
import org.example.backend.service.gateway.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FeatureFallbackBehaviorTest {
    @Test void missingQueriesRemainExplicitlyUnavailableAcrossAdapters() {
        FeatureConfiguration config=new FeatureConfiguration();
        assertEquals(CapabilityStatus.UNAVAILABLE,config.environmentSnapshotPort(config.unavailableEnvironmentSnapshotQuery()).findByTeachingTaskId(1L).status());
        assertEquals(CapabilityStatus.UNAVAILABLE,config.reservationSnapshotPort(config.unavailableReservationSnapshotQuery()).findByTeachingTaskId(1L).status());
        assertEquals(CapabilityStatus.UNAVAILABLE,config.scheduleSnapshotPort(config.unavailableScheduleSnapshotQuery()).findByTeachingTaskId(1L).status());
    }
    @Test void availableViewsPreserveTheirBusinessIdentityAndScheduleBoundaries() {
        FeatureConfiguration config=new FeatureConfiguration();
        assertEquals(new EnvironmentSnapshot(1L,2L,"ready"),config.environmentSnapshotPort(id->CapabilityResult.available(new EnvironmentSnapshotView(id,2L,"ready"))).findByTeachingTaskId(1L).value());
        assertEquals(new ReservationSnapshot(1L,3L,"approved"),config.reservationSnapshotPort(id->CapabilityResult.available(new ReservationSnapshotView(id,3L,"approved"))).findByTeachingTaskId(1L).value());
        Instant start=Instant.parse("2026-10-18T00:00:00Z"),end=start.plusSeconds(3600);
        assertEquals(new ScheduleSnapshot(1L,"course","teacher",start,end),config.scheduleSnapshotPort(id->CapabilityResult.available(new ScheduleSnapshotView(id,"course","teacher",start,end))).findByTeachingTaskId(1L).value());
    }
    @Test void mockGatewayEchoesCommandAndMarksResponsesAsSimulation() {
        MockGatewayAdapter adapter=new MockGatewayAdapter();GatewayCommandRequest request=GatewayCommandRequest.mock(1L,"D1",2L,"light","on",Map.of("enabled",true));
        assertEquals("mock",adapter.protocol());GatewayCommandResult result=adapter.sendCommand(request);assertTrue(result.success());assertEquals("mock_gateway",result.gatewayMode());assertEquals(request.payload(),result.responseResult().get("payload"));assertEquals("D1",result.responseResult().get("iotCode"));assertNotNull(result.finishedAt());
        assertTrue(adapter.queryStatus(request).online());assertEquals("D1",adapter.queryStatus(request).responseResult().get("iotCode"));
        assertEquals("",adapter.queryStatus(GatewayCommandRequest.mock(null,null,null,null,null,null)).responseResult().get("deviceType"));
    }
    @Test void exceptionResponsesHideInternalErrorsAndExplainUploadOrConstraintFailures() {
        GlobalExceptionHandler handler=new GlobalExceptionHandler();MockHttpServletRequest request=new MockHttpServletRequest("POST","/files/avatars");
        assertTrue(handler.handleUploadTooLarge(new MaxUploadSizeExceededException(2),request).getBody().getMessage().contains("头像"));request.setRequestURI("/files/import");assertTrue(handler.handleUploadTooLarge(new MaxUploadSizeExceededException(2),request).getBody().getMessage().contains("服务器"));
        assertEquals(409,handler.handleCrawlerCredentialException(new CrawlerCredentialException(new Exception("private")),request).getStatusCode().value());
        assertEquals("服务暂时不可用，请稍后重试",handler.handleException(new Exception("private database password"),request).getBody().getMessage());
        assertEquals("请求被拒绝",handler.handleResponseStatusException(new ResponseStatusException(HttpStatus.FORBIDDEN),request).getBody().getMessage());
        assertEquals("无权限",handler.handleResponseStatusException(new ResponseStatusException(HttpStatus.FORBIDDEN,"无权限"),request).getBody().getMessage());
        request.setMethod("DELETE");assertTrue(handler.handleDataIntegrityViolationException(new DataIntegrityViolationException("foreign key"),request).getBody().getMessage().contains("解除关联"));
        request.setMethod("POST");request.setRequestURI("/batch/delete");assertTrue(handler.handleDataIntegrityViolationException(new DataIntegrityViolationException("foreign key"),request).getBody().getMessage().contains("解除关联"));
        request.setRequestURI("/records");assertTrue(handler.handleDataIntegrityViolationException(new DataIntegrityViolationException("constraint"),request).getBody().getMessage().contains("格式"));
    }
}

