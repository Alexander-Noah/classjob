package org.example.backend.service;

import org.example.backend.entity.RepairEntity;
import org.example.backend.entity.ReservationsEntity;
import org.example.backend.entity.UsageRecordEntity;
import org.example.backend.mapper.DevicesMapper;
import org.example.backend.mapper.ModuleRecordMapper;
import org.example.backend.mapper.RepairMapper;
import org.example.backend.mapper.UsageRecordMapper;
import org.example.backend.service.impl.BusinessLoopServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BusinessLoopBehaviorTest {
    private final UsageRecordMapper usage = mock(UsageRecordMapper.class);
    private final RepairMapper repairs = mock(RepairMapper.class);
    private final DevicesMapper devices = mock(DevicesMapper.class);
    private final ModuleRecordMapper records = mock(ModuleRecordMapper.class);
    private final ObjectMapper json = new ObjectMapper();
    private final BusinessLoopServiceImpl service = new BusinessLoopServiceImpl(usage, repairs, devices, records, json);

    @Test void approvalInsertsUsageOnceAndRecordsAuditableIdentity() throws Exception {
        service.syncUsageRecordAfterReservationApproved(null); service.syncUsageRecordAfterReservationApproved(new ReservationsEntity());
        verifyNoInteractions(usage);
        var reservation = new ReservationsEntity(); reservation.setId(7L); reservation.setApplicantName("张同学"); reservation.setLabName("机房"); reservation.setReservationDate(LocalDate.of(2027,1,4)); reservation.setTimeRange("08:30-10:00");
        service.syncUsageRecordAfterReservationApproved(reservation);
        var captor = ArgumentCaptor.forClass(UsageRecordEntity.class); verify(usage).insertUsageRecord(captor.capture());
        assertEquals("张同学", captor.getValue().getPerson()); assertEquals("机房", captor.getValue().getResource()); assertEquals("预约", captor.getValue().getScene()); assertEquals("2027-01-04T08:30", captor.getValue().getUseTime().toString());
        assertTrue(captor.getValue().getRemark().contains("7"));
        when(usage.countUsageRecordBySignature(any(),any(),any(),any())).thenReturn(1);
        service.syncUsageRecordAfterReservationApproved(reservation); verify(usage, times(1)).insertUsageRecord(any());
        var event = ArgumentCaptor.forClass(String.class); verify(records,times(2)).insertRecord(eq("operation-events"),anyString(),event.capture());
        assertEquals("approve", json.readTree(event.getValue()).get("action").asText());
    }

    @Test void malformedOrAbsentReservationTimeUsesSafeDateFallback() {
        var reservation = new ReservationsEntity(); reservation.setId(7L); reservation.setApplicantName("张同学"); reservation.setLabName("机房"); reservation.setReservationDate(LocalDate.of(2027,1,4)); reservation.setTimeRange("bad-time");
        service.syncUsageRecordAfterReservationApproved(reservation);
        var captor = ArgumentCaptor.forClass(UsageRecordEntity.class); verify(usage).insertUsageRecord(captor.capture()); assertEquals(reservation.getReservationDate().atStartOfDay(),captor.getValue().getUseTime());
        reservation.setReservationDate(null); service.syncUsageRecordAfterReservationApproved(reservation);
    }

    @Test void abnormalUsageCreatesOneRepairAndSkipsExistingOpenWorkOrders() {
        assertNull(service.createRepairAfterUsageAbnormal(null)); assertNull(service.createRepairAfterUsageAbnormal(new UsageRecordEntity()));
        var record = new UsageRecordEntity(); record.setId(7L); record.setPerson("张同学"); record.setResource("机房"); record.setRemark("设备异常");
        when(repairs.getRepair(anyString())).thenAnswer(call -> { var value = new RepairEntity(); value.setTicket(call.getArgument(0)); return value; });
        assertNotNull(service.createRepairAfterUsageAbnormal(record));
        var captor = ArgumentCaptor.forClass(RepairEntity.class); verify(repairs).insertRepair(captor.capture());
        assertEquals("使用异常", captor.getValue().getFaultType()); assertEquals("待派单", captor.getValue().getStatus()); assertTrue(captor.getValue().getDescription().contains("使用记录#7"));
        when(repairs.countOpenRepairByDescriptionMarker("使用记录#7")).thenReturn(1); assertNull(service.createRepairAfterUsageAbnormal(record)); verify(repairs,times(1)).insertRepair(any());
    }

    @Test void repairStatusSynchronizesAssetStateForCompletionProcessingAndNewFaults() {
        service.syncDeviceAfterRepairChanged(null); service.syncDeviceAfterRepairChanged(new RepairEntity()); verifyNoInteractions(devices);
        var repair = new RepairEntity(); repair.setDevice("DEV-1"); repair.setTicket("R1");
        when(devices.updateDeviceAssetMaintenanceStateByNameOrCode(any(),any(),any())).thenReturn(1);
        for (String[] pair : new String[][]{{"已完成","正常"},{"待验收","维护中"},{"处理中","维护中"},{"待派单","故障"}}) {
            repair.setStatus(pair[0]); service.syncDeviceAfterRepairChanged(repair);
            verify(devices,atLeastOnce()).updateDeviceAssetMaintenanceStateByNameOrCode(eq("DEV-1"),eq(pair[1]),anyString());
        }
        verify(records,times(4)).insertRecord(any(),any(),any());
    }

    @Test void eventDefaultsAndDetailsSerializeAndLoggingFailureDoesNotBreakBusinessWork() throws Exception {
        service.recordEvent(null,null,null,null,null);
        var captor = ArgumentCaptor.forClass(String.class); verify(records).insertRecord(eq("operation-events"),anyString(),captor.capture());
        var event = json.readTree(captor.getValue()); assertEquals("business",event.get("category").asText()); assertEquals("operate",event.get("action").asText()); assertEquals("-",event.get("subject").asText()); assertEquals("完成",event.get("status").asText());
        doThrow(new IllegalStateException("test audit failure")).when(records).insertRecord(any(),any(),any());
        assertDoesNotThrow(() -> service.recordEvent("test","save","subject","ok",Map.of("id",7)));
    }
}
