package org.example.backend.config;
import org.example.backend.entity.ReservationStatus;
import org.example.backend.mapper.*;
import org.example.backend.service.*;
import org.example.backend.service.gateway.*;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationStorageAdapterTest {
    @Test void statusHandlerUsesLegacyChineseValuesAndRejectsUnknownDatabaseStates() throws Exception{var handler=new ReservationStatusTypeHandler();PreparedStatement prepared=mock(PreparedStatement.class);ResultSet rows=mock(ResultSet.class);CallableStatement call=mock(CallableStatement.class);for(ReservationStatus status:ReservationStatus.values()){handler.setNonNullParameter(prepared,1,status,null);verify(prepared).setString(1,status.storageValue());when(rows.getString("status")).thenReturn(status.storageValue());when(rows.getString(1)).thenReturn(status.storageValue());when(call.getString(1)).thenReturn(status.storageValue());assertEquals(status,handler.getNullableResult(rows,"status"));assertEquals(status,handler.getNullableResult(rows,1));assertEquals(status,handler.getNullableResult(call,1));assertEquals(!Set.of(ReservationStatus.PENDING,ReservationStatus.CONFLICT,ReservationStatus.COUNTERSIGNING).contains(status),status.isFinal());}when(rows.getString(1)).thenReturn(null);assertNull(handler.getNullableResult(rows,1));when(rows.getString(1)).thenReturn("unknown");assertThrows(IllegalArgumentException.class,()->handler.getNullableResult(rows,1));assertThrows(IllegalArgumentException.class,()->ReservationStatus.fromStorageValue(null));}
    @Test void unconfiguredRealGatewayCannotReportSimulatedHardwareSuccess(){GatewayCommandRequest request=GatewayCommandRequest.mock(null,null,null,null,null,null);for(GatewayAdapter adapter:List.of(new HttpGatewayAdapter(),new MqttGatewayAdapter())){assertFalse(adapter.sendCommand(request).success());assertEquals("real_gateway_unconfigured",adapter.sendCommand(request).gatewayMode());assertFalse(adapter.queryStatus(request).online());assertEquals(adapter.protocol(),adapter.queryStatus(request).responseResult().get("protocol"));}}
    @Test void schedulerOwnsDedicatedThreadAndSupportsCancellation(){var scheduler=new ClassTimetableSchedulerConfig().classTimetableTaskScheduler();try{assertEquals(1,scheduler.getScheduledThreadPoolExecutor().getCorePoolSize());assertEquals("class-timetable-crawler-",scheduler.getThreadNamePrefix());assertTrue(scheduler.getScheduledThreadPoolExecutor().getRemoveOnCancelPolicy());}finally{scheduler.shutdown();}}
    @Test void labReadServicePreservesManagerScopeAndReturnsEmptyForMissingCollections(){LabRecordReadMapper mapper=mock(LabRecordReadMapper.class);LabRecordReadService service=new LabRecordReadService(mapper);assertEquals(List.of(),service.findAll(7L));service.findById(1L,7L);verify(mapper).findAll(7L);verify(mapper).findById(1L,7L);assertThrows(NullPointerException.class,()->new LabRecordReadService(null));}
}

