package org.example.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiRuntimeDataBoundaryTest {
    @Test void runtimeQueriesRetainOperationalDevicesButRemoveConnectionSecretsAndAssetPayloads() {
        ApplicationContext beans=mock(ApplicationContext.class);
        DeviceStatusService runtime=mock(DeviceStatusService.class);
        when(beans.getBean(DeviceStatusService.class)).thenReturn(runtime);
        when(runtime.listLabRuntimeStatus(eq(7L),anyMap())).thenReturn(Map.of("devices",List.of(Map.of(
                "id",1,"labId",7,"runtimeStatus","online","streamUrl","rtsp://private","connectionConfig","private",
                "devices",List.of(Map.of("id",2,"runtimeStatus","offline"))))));
        AiBusinessDataService service=new AiBusinessDataService(beans);
        AiDataActor actor=new AiDataActor(1,"systemAdmin","Admin",Set.of());
        Map<String,Object> result=service.query(actor,Map.of("dataset","runtime-status","labId",7));
        assertEquals("ok",result.get("status"));assertEquals(1,result.get("total"));
        String encoded=service.encode(result);
        assertTrue(encoded.contains("online"));assertTrue(encoded.contains("offline"));assertFalse(encoded.contains("private"));
        ModuleRecordService modules=mock(ModuleRecordService.class);when(beans.getBean(ModuleRecordService.class)).thenReturn(modules);
        when(modules.listModuleRecords("operation-events")).thenReturn(List.of(Map.of("id",1,"devices",List.of(Map.of("name","asset-secret")),"note","ordinary")));
        String events=service.encode(service.query(actor,Map.of("dataset","operation-events")));
        assertTrue(events.contains("ordinary"));assertFalse(events.contains("asset-secret"));
        verify(runtime).listLabRuntimeStatus(eq(7L),anyMap());
    }
}
