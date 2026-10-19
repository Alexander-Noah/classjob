package org.example.backend.service;

import org.example.backend.entity.ConsumableEntity;
import org.example.backend.entity.ConsumableStockRecordEntity;
import org.example.backend.entity.NoticeEntity;
import org.example.backend.mapper.ConsumableMapper;
import org.example.backend.mapper.NoticeMapper;
import org.example.backend.service.impl.ConsumableServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConsumableStockBehaviorTest {
    private final ConsumableMapper mapper = mock(ConsumableMapper.class);
    private final NoticeMapper notices = mock(NoticeMapper.class);
    private final BusinessLoopService events = mock(BusinessLoopService.class);
    private final ConsumableServiceImpl service = new ConsumableServiceImpl(mapper, notices, events);
    private ConsumableEntity item;
    @BeforeEach
    void setup() {
        item = new ConsumableEntity();
        item.setId(1L); item.setName("网线"); item.setCategory("网络耗材");
        item.setStock(20); item.setWarnThreshold(10); item.setUnit("根"); item.setTagType("success");
        when(mapper.getConsumable(1L)).thenReturn(item);
        when(mapper.getConsumableForUpdate(1L)).thenReturn(item);
        when(mapper.listConsumables()).thenReturn(List.of(item));
        when(mapper.updateConsumableStock(anyLong(), anyInt(), anyString(), anyString())).thenReturn(1);
    }

    @Test
    void emptyCreateAppliesDefaultsAndCrudUsesSavedRecords() {
        service.createConsumable(null);
        ArgumentCaptor<ConsumableEntity> captor = ArgumentCaptor.forClass(ConsumableEntity.class);
        verify(mapper).insertConsumable(captor.capture());
        var created = captor.getValue();
        assertEquals("新耗材", created.getName());
        assertEquals("教学耗材", created.getCategory());
        assertEquals("件", created.getUnit());
        assertEquals(0, created.getStock());
        assertEquals(10, created.getWarnThreshold());
        assertEquals("待补货", created.getStatus());
        assertEquals("danger", created.getTagType());
        assertSame(item, service.updateConsumable(1L, item));
        assertEquals("库存充足", item.getStatus());
        assertEquals(List.of(item), service.listConsumables());
        assertEquals(List.of(item), service.deleteConsumable(1L));
        verify(mapper).deleteConsumable(1L);
        assertEquals(List.of(), service.listStockRecords(1L));
        ConsumableStockRecordEntity record = new ConsumableStockRecordEntity();
        when(mapper.listStockRecords(1L)).thenReturn(List.of(record));
        assertEquals(List.of(record), service.listStockRecords(1L));
    }

    @Test
    void stockStatusDistinguishesZeroLowAndSufficientAndPublishesWarnings() {
        for (int stock : new int[]{0, 5, 20}) {
            item.setStock(stock);
            service.updateConsumable(1L, item);
            assertEquals(stock == 0 ? "danger" : stock <= 10 ? "warning" : "success", item.getTagType());
        }
        ArgumentCaptor<NoticeEntity> captor = ArgumentCaptor.forClass(NoticeEntity.class);
        verify(notices, times(2)).insertNotice(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(notice -> "labAdmin".equals(notice.getTargetRole()) && "已发布".equals(notice.getPublishStatus()) && notice.getPublishTime() != null));
        assertTrue(captor.getValue().getContent().contains("5根"));
    }

    @Test
    void historicalLoansBlockIndividualAndBulkDestructionBeforeWrites() {
        when(mapper.countLoanRecords(any())).thenReturn(1);
        assertThrows(IllegalArgumentException.class, () -> service.deleteConsumable(1L));
        assertThrows(IllegalArgumentException.class, () -> service.replaceConsumables(List.of()));
        assertThrows(IllegalArgumentException.class, service::resetConsumables);
        verify(mapper, never()).deleteConsumable(any());
        verify(mapper, never()).deleteAllConsumables();
    }

    @Test
    void replacementAndResetPreserveTheirDeclaredContents() {
        assertEquals(List.of(item), service.replaceConsumables(null));
        verify(mapper).deleteAllConsumables();
        service.replaceConsumables(List.of(item));
        verify(mapper).insertConsumable(item);
        clearInvocations(mapper);
        service.resetConsumables();
        ArgumentCaptor<ConsumableEntity> captor = ArgumentCaptor.forClass(ConsumableEntity.class);
        verify(mapper, times(3)).insertConsumable(captor.capture());
        assertEquals(List.of("网线", "鼠标", "固态硬盘"), captor.getAllValues().stream().map(ConsumableEntity::getName).toList());
        assertEquals(List.of("success", "warning", "warning"), captor.getAllValues().stream().map(ConsumableEntity::getTagType).toList());
    }

    @Test
    void stockInAndOutUseLockedStockAndWriteAuditWithDefaults() {
        var incoming = movement(" stock-in ", 5);
        service.recordConsumableMovement(1L, incoming);
        assertEquals(25, item.getStock());
        assertEquals("入库", incoming.getType());
        assertEquals(20, incoming.getBeforeStock());
        assertEquals(25, incoming.getAfterStock());
        assertEquals("网线", incoming.getConsumableName());
        assertEquals("根", incoming.getUnit());
        assertEquals("system", incoming.getOperator());
        assertEquals("-", incoming.getSource());
        assertEquals("-", incoming.getReason());
        assertEquals("", incoming.getRemark());
        assertNotNull(incoming.getTime());
        verify(mapper).insertStockRecord(incoming);
        var outgoing = movement("OUT", 20);
        outgoing.setOperator(" 管理员 "); outgoing.setUnit(" 条 "); outgoing.setSource(" 领用 "); outgoing.setReason(" 教学 "); outgoing.setRemark(" 备注 ");
        service.recordConsumableMovement(1L, outgoing);
        assertEquals(5, item.getStock());
        assertEquals("低库存", item.getStatus());
        assertEquals("出库", outgoing.getType());
        assertEquals("管理员", outgoing.getOperator());
        assertEquals("条", outgoing.getUnit());
        assertEquals("领用", outgoing.getSource());
        assertEquals("教学", outgoing.getReason());
        assertEquals("备注", outgoing.getRemark());
        var order = inOrder(mapper);
        order.verify(mapper).getConsumableForUpdate(1L);
        order.verify(mapper).updateConsumableStock(1L, 25, "库存充足", "success");
        order.verify(mapper).insertStockRecord(incoming);
    }

    @Test
    void invalidMovementsRejectBeforeStockOrAuditWrites() {
        assertThrows(IllegalArgumentException.class, () -> service.recordConsumableMovement(null, movement("in", 1)));
        assertThrows(IllegalArgumentException.class, () -> service.recordConsumableMovement(999L, movement("in", 1)));
        assertThrows(IllegalArgumentException.class, () -> service.recordConsumableMovement(1L, null));
        for (int quantity : new int[]{0, -1}) assertThrows(IllegalArgumentException.class, () -> service.recordConsumableMovement(1L, movement("in", quantity)));
        for (String type : new String[]{null, "invalid"}) assertThrows(IllegalArgumentException.class, () -> service.recordConsumableMovement(1L, movement(type, 1)));
        assertThrows(IllegalArgumentException.class, () -> service.recordConsumableMovement(1L, movement("out", 21)));
        item.setStock(Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> service.recordConsumableMovement(1L, movement("in", 1)));
        verify(mapper, never()).updateConsumableStock(any(), any(), any(), any());
        verify(mapper, never()).insertStockRecord(any());
    }

    @Test
    void vanishedItemDoesNotWriteAuditAndNullStockIsTreatedAsZero() {
        when(mapper.updateConsumableStock(any(), any(), any(), any())).thenReturn(0);
        assertThrows(IllegalArgumentException.class, () -> service.recordConsumableMovement(1L, movement("入库", 1)));
        verify(mapper, never()).insertStockRecord(any());
        when(mapper.updateConsumableStock(any(), any(), any(), any())).thenReturn(1);
        item.setStock(null);
        var request = movement("入库", 2);
        service.recordConsumableMovement(1L, request);
        assertEquals(0, request.getBeforeStock());
        assertEquals(2, request.getAfterStock());
        service.recordConsumableMovement(1L, movement("出库", 2));
        assertEquals("待补货", item.getStatus());
    }

    private ConsumableStockRecordEntity movement(String type, int quantity) {
        var record = new ConsumableStockRecordEntity(); record.setType(type); record.setQuantity(quantity); return record;
    }
}
