package org.example.backend.service;
import org.example.backend.entity.*;
import org.example.backend.mapper.*;
import org.example.backend.service.impl.DevicesServiceImpl;
import org.example.backend.service.DevicesService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeviceLifecycleBehaviorTest {
    final DevicesMapper mapper=mock(DevicesMapper.class);
    final DeviceEventMapper events=mock(DeviceEventMapper.class);
    final LabMapper labs=mock(LabMapper.class);
    final BusinessLoopService loop=mock(BusinessLoopService.class);
    final DevicesService service=new DevicesServiceImpl(mapper,events,labs,loop);
    final Map<Long,DevicesEntity> store=new HashMap<>();
    DevicesEntity device(){DevicesEntity device=new DevicesEntity();device.setId(1L);device.setDeviceName("教学电脑");device.setDeviceCode("PC1");device.setLabId(7L);device.setLabName("A101");device.setOwnerUserId(8L);device.setOwnerUsername("管理员");device.setLocation("A101-1");device.setStatus("正常");return device;}
    @BeforeEach void setup(){store.put(1L,device());when(mapper.getDevicesById(anyLong())).thenAnswer(call->store.get(call.getArgument(0)));when(mapper.updateDevices(anyLong(),any())).thenAnswer(call->{if(!store.containsKey(call.getArgument(0)))return 0;store.put(call.getArgument(0),call.getArgument(1));return 1;});when(mapper.getDevices()).thenAnswer(call->new ArrayList<>(store.values()));}
    @Test void labBindingPrioritizesIdsThenRoomNamesAndImportText(){LabEntity lab=new LabEntity();lab.setId(7);lab.setLabName("开发机房");lab.setRoomNo("A101");when(labs.getLabs()).thenReturn(List.of(lab));for(int mode=0;mode<7;mode++){DevicesEntity input=new DevicesEntity();input.setDeviceName("电脑");switch(mode){case 0->input.setLabId(7L);case 1->input.setLocation("A101");case 2->input.setLabName("开发机房");case 3->input.setLabName("开发机房一室");case 4->input.setLabName("开发");case 5->input.setLocation("实验楼A101室");case 6->input.setRemark("开发机房教学用");}assertSame(input,service.InserterDevices(input));assertEquals(7L,input.getLabId());assertEquals("开发机房",input.getLabName());assertEquals("A101",input.getLocation());}DevicesEntity unrelated=new DevicesEntity();unrelated.setDeviceName("路由器");unrelated.setLabId(99L);unrelated.setLocation("B202");unrelated.setLabName("未知机房");service.InserterDevices(unrelated);assertEquals(99L,unrelated.getLabId());assertEquals("B202",unrelated.getLocation());assertSame(store.get(1L),service.getDevicesById(1L));}
    @Test void paginationClampsBoundsAndPassesCleanFilters(){DevicePageQuery query=new DevicePageQuery();query.setPageNum(-1);query.setPageSize(200000);query.setKeyword(" 电脑 ");query.setLabName(" ");when(mapper.countDevices(any(),any(),any(),any(),any())).thenReturn(23L);DevicePageResult result=service.pageDevices(query);assertEquals(23L,result.getTotal());assertEquals(List.of(),result.getRecords());verify(mapper).pageDevices(0,100000,"电脑",null,null,null,null);assertEquals(1,query.getPageNum());assertEquals(100000,query.getPageSize());service.pageDevices(null);}
    @Test void statsNormalizeDatabaseNumericTypesAndInvalidValues(){when(mapper.getDeviceStats()).thenReturn(Map.of("total","12","normal",10,"repairing","bad","idle",2L));assertEquals(Map.of("total",12L,"normal",10L,"repairing",0L,"idle",2L,"scrapped",0L),service.getDeviceStats());when(mapper.getDeviceStats()).thenReturn(null);assertTrue(service.getDeviceStats().values().stream().allMatch(value->Long.valueOf(0).equals(value)));}
    @Test void creationAndUpdatesNormalizeDefaultsAndRejectMissingNames(){DevicesEntity input=new DevicesEntity();input.setDeviceName("投影仪");input.setStatus("unknown");assertSame(input,service.InserterDevices(input));assertNotNull(input.getDeviceCode());assertEquals("显示设备",input.getCategory());assertEquals("正常",input.getStatus());assertEquals(1,input.getQuantity());assertEquals("台",input.getUnit());assertEquals("良好",input.getHealth());assertTrue(input.getOnline());assertThrows(IllegalArgumentException.class,()->service.InserterDevices(null));assertThrows(IllegalArgumentException.class,()->service.InserterDevices(new DevicesEntity()));DevicesEntity updated=device();updated.setStatus("维修中");assertSame(updated,service.updateDevices(1L,updated));assertThrows(IllegalArgumentException.class,()->service.updateDevices(99L,device()));service.updateDevices(null);service.updateDevices(List.of());service.updateDevices(List.of(updated,input));verify(mapper,times(2)).updateDevices(1L,updated);}
    @Test void inventoryUsesCanonicalDeviceAndWritesDateOnlyState(){DeviceInventoryRecordEntity input=new DeviceInventoryRecordEntity();input.setDeviceId(99L);input.setInspectedAt("2026-10-18 10:00:00");var result=service.recordDeviceInventory(1L,input);assertSame(store.get(1L),result.getDevice());assertEquals(1L,input.getDeviceId());assertEquals("PC1",input.getDeviceCode());assertEquals("lab admin",input.getInspectorName());assertEquals("已盘点",input.getResultStatus());assertFalse(input.getNormal());assertEquals(List.of(),result.getRecords());verify(mapper).updateInventoryState(1L,"2026-10-18");verify(events).insertInventoryEvent(input);assertThrows(IllegalArgumentException.class,()->service.recordDeviceInventory(null,input));assertThrows(IllegalArgumentException.class,()->service.recordDeviceInventory(99L,input));assertEquals(List.of(),service.listInventoryRecords(1L));}
    @Test void transferSnapshotsSourceAndPreservesLocationWhenDestinationLocationIsOmitted(){DeviceTransferRecordEntity input=new DeviceTransferRecordEntity();input.setToLabId(9L);input.setToOwnerUserId(10L);var result=service.transferDevice(1L,input);assertEquals(7L,input.getFromLabId());assertEquals("A101",input.getFromLabName());assertEquals(8L,input.getFromOwnerUserId());assertEquals("管理员",input.getFromOwnerName());assertEquals("A101-1",input.getToLocation());assertEquals("设备调拨",input.getTransferType());assertEquals(List.of(),result.getRecords());verify(mapper).updateTransferState(1L,9L,10L,"A101-1");verify(events).insertTransferEvent(input);assertEquals(List.of(),service.listTransferRecords(1L));}
    @Test void deletionAuditsExistingDeviceAndMissingListsRemainEmpty(){service.deleteDevices(1L);verify(mapper).deleteDevices(1L);verify(loop).recordEvent(eq("device"),eq("delete"),eq("教学电脑"),eq("已报废"),anyMap());service.deleteDevices(99L);assertEquals(List.of(),service.getDevicesByLabId(null));assertEquals(List.of(),service.getDevicesByLabId(7));when(mapper.getDevices()).thenReturn(null);assertEquals(List.of(),service.getDevices());}
    @Test void exportedCsvIncludesBomEscapesQuotesAndUsesFullPageScope(){DevicesEntity record=device();record.setDeviceName("电脑,\"教学\"");record.setRemark("第一行\n第二行");when(mapper.pageDevices(anyInt(),anyInt(),any(),any(),any(),any(),any())).thenReturn(List.of(record));String csv=new String(service.exportDevices(null),StandardCharsets.UTF_8);assertTrue(csv.startsWith("\ufeffassetNo,deviceName"));assertTrue(csv.contains("\"电脑,\"\"教学\"\"\""));assertTrue(csv.contains("\"第一行\n第二行\""));verify(mapper).pageDevices(0,100000,null,null,null,null,null);}
    @Test void existingInventoryAndTransferHistoryAreReturnedWithShortDateFallback() {
        var inventory=new DeviceInventoryRecordEntity();inventory.setInspectedAt("today");inventory.setNormal(true);inventory.setMissing(true);inventory.setDamaged(true);
        var history=List.of(inventory);when(events.listInventoryEvents(1L)).thenReturn(history);
        assertSame(history,service.recordDeviceInventory(1L,inventory).getRecords());assertSame(history,service.listInventoryRecords(1L));
        verify(mapper).updateInventoryState(1L,java.time.LocalDate.now().toString());
        var transfer=new DeviceTransferRecordEntity();transfer.setToLocation("B202");var transfers=List.of(transfer);when(events.listTransferEvents(1L)).thenReturn(transfers);
        assertSame(transfers,service.transferDevice(1L,transfer).getRecords());assertSame(transfers,service.listTransferRecords(1L));
        verify(mapper).updateTransferState(1L,null,null,"B202");
        when(mapper.getDevicesByLabId(7)).thenReturn(List.of(device()));assertEquals(1,service.getDevicesByLabId(7).size());
        when(mapper.pageDevices(anyInt(),anyInt(),any(),any(),any(),any(),any())).thenReturn(List.of(device()));assertEquals(1,service.pageDevices(null).getRecords().size());
    }
    @Test void disappearedRowsDoNotEmitMisleadingCreateOrUpdateAuditEvents() {
        DevicesEntity absent=device();absent.setId(99L);assertNull(service.InserterDevices(absent));
        when(mapper.updateDevices(eq(99L),any())).thenReturn(1);assertNull(service.updateDevices(99L,absent));verifyNoInteractions(loop);
    }
    @Test void suppliedAssetDefaultsAndValidStatusesArePreservedEvenWithoutLabCatalog() {
        DevicesService detached=new DevicesServiceImpl(mapper,events,null,loop);
        for(String status:List.of("闲置","已报废","维修中")) {
            DevicesEntity input=device();input.setId(null);input.setStatus(status);input.setQuantity(0);input.setOnline(false);input.setUsageHours(24);input.setSourceType("import");input.setDeleted(0);input.setHealth("待检查");input.setCategory("其他");input.setUnit("套");
            assertSame(input,detached.InserterDevices(input));assertEquals(status,input.getStatus());assertEquals(1,input.getQuantity());assertFalse(input.getOnline());assertEquals(24,input.getUsageHours());assertEquals("套",input.getUnit());
        }
        LabEntity anonymous=new LabEntity();anonymous.setLabName("开发机房");anonymous.setRoomNo("A101");when(labs.getLabs()).thenReturn(List.of(anonymous));
        DevicesEntity input=new DevicesEntity();input.setDeviceName("电脑");input.setLabName("开发机房");assertNull(service.InserterDevices(input).getLabId());
    }
    @Test void auditPersistenceFailureDoesNotUndoSuccessfulAssetInsert() {
        doThrow(new IllegalStateException("audit unavailable")).when(loop).recordEvent(anyString(),anyString(),anyString(),anyString(),anyMap());
        DevicesEntity input=new DevicesEntity();input.setDeviceName("电脑");assertSame(input,service.InserterDevices(input));verify(mapper).InserterDevices(input);
    }
}
