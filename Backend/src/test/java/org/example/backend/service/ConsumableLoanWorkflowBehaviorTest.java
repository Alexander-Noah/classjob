package org.example.backend.service;
import org.example.backend.entity.*;
import org.example.backend.mapper.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsumableLoanWorkflowBehaviorTest {
    final ConsumableLoanMapper loans=mock(ConsumableLoanMapper.class);final ConsumableMapper items=mock(ConsumableMapper.class);final ConsumableService stock=mock(ConsumableService.class);final ConsumableLoanService service=new ConsumableLoanService(loans,items,stock);
    ConsumableLoanService.BorrowRequest request(){return new ConsumableLoanService.BorrowRequest(1L," 教师甲 "," 123 ",BigDecimal.valueOf(2)," 教学 ",LocalDate.now().plusDays(1)," 备注 ",UUID.randomUUID().toString());}
    @Test void successfulBorrowAndPartialReturnWriteMatchingInventoryMovements(){ConsumableEntity item=new ConsumableEntity();item.setId(1L);item.setName("网线");item.setUnit("条");item.setStock(5);when(items.getConsumableForUpdate(1L)).thenReturn(item);ConsumableLoanEntity saved=new ConsumableLoanEntity();saved.setId(7L);saved.setConsumableId(1L);saved.setBorrower("教师甲");saved.setRemainingQuantity(2);when(loans.get(7L)).thenReturn(saved);doAnswer(call->{((ConsumableLoanEntity)call.getArgument(0)).setId(7L);return 1;}).when(loans).insert(any());assertSame(saved,service.borrow(request(),"管理员"));var loan=ArgumentCaptor.forClass(ConsumableLoanEntity.class);verify(loans).insert(loan.capture());assertEquals("教师甲",loan.getValue().getBorrower());assertEquals("教学",loan.getValue().getPurpose());verify(stock).recordConsumableMovement(eq(1L),argThat(record->"出库".equals(record.getType())&&record.getQuantity()==2&&"借用#7".equals(record.getSource())));when(loans.lock(7L)).thenReturn(saved);when(loans.addReturned(7L,1)).thenReturn(1);assertSame(saved,service.returnItems(7L,new ConsumableLoanService.ReturnRequest(BigDecimal.ONE," 完好 ",UUID.randomUUID().toString()),"管理员"));verify(stock).recordConsumableMovement(eq(1L),argThat(record->"入库".equals(record.getType())&&record.getQuantity()==1&&"完好".equals(record.getRemark())));when(loans.list()).thenReturn(List.of(saved));assertEquals(List.of(saved),service.list());}
    @Test void invalidRequestsMissingStockAndStaleReturnsNeverMoveInventory(){assertThrows(IllegalArgumentException.class,()->service.borrow(null,"admin"));assertThrows(IllegalArgumentException.class,()->service.returnItems(7L,null,"admin"));assertThrows(ResponseStatusException.class,()->service.get(7L));assertThrows(ResponseStatusException.class,()->service.borrow(request(),"admin"));ConsumableEntity item=new ConsumableEntity();item.setStock(0);when(items.getConsumableForUpdate(1L)).thenReturn(item);assertThrows(IllegalArgumentException.class,()->service.borrow(request(),"admin"));ConsumableLoanEntity loan=new ConsumableLoanEntity();loan.setRemainingQuantity(2);when(loans.lock(7L)).thenReturn(loan);assertThrows(ResponseStatusException.class,()->service.returnItems(7L,new ConsumableLoanService.ReturnRequest(BigDecimal.ONE,"",UUID.randomUUID().toString()),"admin"));verifyNoInteractions(stock);}
    @Test void reusedBorrowOrReturnRequestCannotBeAppliedToDifferentPayload(){ConsumableEntity item=new ConsumableEntity();item.setStock(5);when(items.getConsumableForUpdate(1L)).thenReturn(item);var request=request();when(loans.byRequest(request.requestId())).thenReturn(new ConsumableLoanEntity());assertThrows(ResponseStatusException.class,()->service.borrow(request,"admin"));ConsumableLoanEntity loan=new ConsumableLoanEntity();loan.setConsumableId(1L);when(loans.lock(7L)).thenReturn(loan);ConsumableStockRecordEntity previous=new ConsumableStockRecordEntity();previous.setQuantity(2);when(items.getStockRecordBySource(any())).thenReturn(previous);assertThrows(ResponseStatusException.class,()->service.returnItems(7L,new ConsumableLoanService.ReturnRequest(BigDecimal.ONE,"",UUID.randomUUID().toString()),"admin"));verifyNoInteractions(stock);}
    @Test void replayedBorrowKeysMustMatchEveryBusinessFieldWithoutMovingStockAgain() {
        var request=request();var item=new ConsumableEntity();item.setStock(5);when(items.getConsumableForUpdate(1L)).thenReturn(item);
        for(int field=0;field<9;field++) {
            var previous=new ConsumableLoanEntity();previous.setConsumableId(1L);previous.setQuantity(2);previous.setBorrower("教师甲");previous.setPurpose("教学");previous.setDueDate(request.dueDate().toString());previous.setOperator("管理员");previous.setContact("123");previous.setRemark("备注");
            switch(field) {case 0->previous.setConsumableId(2L);case 1->previous.setQuantity(3);case 2->previous.setBorrower("其他人");case 3->previous.setPurpose("其他用途");case 4->previous.setDueDate("2000-01-01");case 5->previous.setOperator("其他管理员");case 6->previous.setContact("456");case 7->previous.setRemark("其他备注");}
            when(loans.byRequest(request.requestId())).thenReturn(previous);
            if(field==8)assertSame(previous,service.borrow(request,"管理员"));else assertThrows(ResponseStatusException.class,()->service.borrow(request,"管理员"));
        }
        verify(loans,never()).insert(any());verifyNoInteractions(stock);
    }
    @Test void replayedReturnKeysMustMatchConsumableOperatorAndRemarks() {
        var loan=new ConsumableLoanEntity();loan.setConsumableId(1L);when(loans.lock(7L)).thenReturn(loan);when(loans.get(7L)).thenReturn(loan);
        var request=new ConsumableLoanService.ReturnRequest(BigDecimal.ONE,"完好",UUID.randomUUID().toString());
        for(int field=0;field<4;field++) {
            var previous=new ConsumableStockRecordEntity();previous.setQuantity(1);previous.setConsumableId(1L);previous.setOperator("管理员");previous.setRemark("完好");
            if(field==0)previous.setConsumableId(2L);else if(field==1)previous.setOperator("其他管理员");else if(field==2)previous.setRemark("损坏");
            when(items.getStockRecordBySource(anyString())).thenReturn(previous);
            if(field==3)assertSame(loan,service.returnItems(7L,request,"管理员"));else assertThrows(ResponseStatusException.class,()->service.returnItems(7L,request,"管理员"));
        }
        verify(loans,never()).addReturned(anyLong(),anyInt());verifyNoInteractions(stock);
    }
    @Test void missingDatesInvalidKeysAmountsAndOversizedFieldsAreRejectedBeforeInventoryChanges() {
        for(String key:Arrays.asList(null,"bad","1-1-1-1-1"))assertThrows(IllegalArgumentException.class,()->service.returnItems(7L,new ConsumableLoanService.ReturnRequest(BigDecimal.ONE,null,key),"管理员"));
        for(BigDecimal quantity:Arrays.asList(null,BigDecimal.ZERO,BigDecimal.valueOf(-1),new BigDecimal("1.5"),new BigDecimal("999999999999")))assertThrows(IllegalArgumentException.class,()->service.returnItems(7L,new ConsumableLoanService.ReturnRequest(quantity,null,UUID.randomUUID().toString()),"管理员"));
        var request=request();
        assertThrows(IllegalArgumentException.class,()->service.borrow(new ConsumableLoanService.BorrowRequest(null,"甲","",BigDecimal.ONE,"教学",request.dueDate(),"",request.requestId()),"管理员"));
        assertThrows(IllegalArgumentException.class,()->service.borrow(new ConsumableLoanService.BorrowRequest(1L," ","",BigDecimal.ONE,"教学",request.dueDate(),"",request.requestId()),"管理员"));
        assertThrows(IllegalArgumentException.class,()->service.borrow(new ConsumableLoanService.BorrowRequest(1L,"甲","",BigDecimal.ONE,"教学",null,"",request.requestId()),"管理员"));
        assertThrows(IllegalArgumentException.class,()->service.returnItems(7L,new ConsumableLoanService.ReturnRequest(BigDecimal.ONE,"长".repeat(501),UUID.randomUUID().toString()),"管理员"));
        var item=new ConsumableEntity();when(items.getConsumableForUpdate(1L)).thenReturn(item);assertThrows(IllegalArgumentException.class,()->service.borrow(request,"管理员"));
        item.setStock(5);var expired=new ConsumableLoanService.BorrowRequest(1L,"甲","",BigDecimal.ONE,"教学",LocalDate.now().minusDays(1),"",UUID.randomUUID().toString());
        assertThrows(IllegalArgumentException.class,()->service.borrow(expired,"管理员"));verifyNoInteractions(stock);
    }
}
