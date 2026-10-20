package org.example.backend.service;
import org.example.backend.entity.MaintenanceRecord;
import org.example.backend.mapper.MaintenanceRecordMapper;
import org.example.backend.service.impl.MaintenanceRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class MaintenanceBoundaryTest {
 private final MaintenanceRecordMapper mapper=mock(MaintenanceRecordMapper.class);
 private final MaintenanceRecordServiceImpl service=new MaintenanceRecordServiceImpl(mapper);
 private MaintenanceRecord valid(){var r=new MaintenanceRecord();r.setId("7");r.setLabId(1L);r.setMaintenanceType("巡检");r.setMaintenanceDate("2026-10-20");r.setMaintainer("管理员");r.setContent("检查线路");r.setResult("正常");return r;}
 @Test void nullCollectionsAndRacingDeletesDoNotPretendSuccess(){
  when(mapper.listMaintenanceRecords()).thenReturn(null);assertTrue(service.listMaintenanceRecords().isEmpty());
  var r=valid();when(mapper.getMaintenanceRecord("7")).thenReturn(r);when(mapper.findLabId("1")).thenReturn(1L);
  var update=assertThrows(ResponseStatusException.class,()->service.updateMaintenanceRecord("7",r));assertEquals(404,update.getStatusCode().value());
  var delete=assertThrows(ResponseStatusException.class,()->service.deleteMaintenanceRecord("7"));assertEquals(404,delete.getStatusCode().value());
 }
 @Test void invalidDatesAndMissingRecordsAreRejectedBeforeWriting(){
  assertThrows(IllegalArgumentException.class,()->service.createMaintenanceRecord(null));
  for(String date:new String[]{"2026-02-30","2026-10-20 25:00:00","not-a-date"}){var r=valid();r.setMaintenanceDate(date);assertTrue(assertThrows(IllegalArgumentException.class,()->service.createMaintenanceRecord(r)).getMessage().contains("维护日期"));}
  verify(mapper,never()).insertMaintenanceRecord(any(),any(),any());
 }
 @Test void labAndDeviceIdentifiersMustMatchResolvedReferences(){
  var r=valid();when(mapper.findLabId("1")).thenReturn(null,2L,1L,1L,1L);
  assertThrows(IllegalArgumentException.class,()->service.createMaintenanceRecord(r));assertThrows(IllegalArgumentException.class,()->service.createMaintenanceRecord(r));
  r.setDeviceId(3L);when(mapper.findDeviceId("3",1L)).thenReturn(null,4L,3L);
  assertThrows(IllegalArgumentException.class,()->service.createMaintenanceRecord(r));assertThrows(IllegalArgumentException.class,()->service.createMaintenanceRecord(r));
  when(mapper.getMaintenanceRecord(anyString())).thenReturn(r);assertSame(r,service.createMaintenanceRecord(r));verify(mapper).insertMaintenanceRecord(r,1L,3L);
 }
 @Test void legacyLabelsAndExplicitNoDeviceRemainSupported(){
  for(String device:new String[]{null," ","未填写","未关联设备","终端"}){
   var r=valid();r.setLabId(null);r.setLab(" A101 ");r.setDevice(device);r.setMaintenanceDate("2026-10-20 08:00:00");r.setRemark("");
   when(mapper.findLabId("A101")).thenReturn(1L);when(mapper.findDeviceId("终端",1L)).thenReturn(3L);when(mapper.getMaintenanceRecord(anyString())).thenReturn(r);
   assertSame(r,service.createMaintenanceRecord(r));verify(mapper).insertMaintenanceRecord(r,1L,"终端".equals(device)?3L:null);
  }
 }
 @Test void longOptionalRemarksAreRejected(){var r=valid();r.setRemark("x".repeat(101));assertTrue(assertThrows(IllegalArgumentException.class,()->service.createMaintenanceRecord(r)).getMessage().contains("备注"));verifyNoInteractions(mapper);}
}
