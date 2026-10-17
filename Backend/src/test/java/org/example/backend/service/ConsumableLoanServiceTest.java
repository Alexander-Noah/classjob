/*
 * 耗材借还回归测试；测试夹具与生产配置分离。
 * 覆盖场景：fractionalZeroAndOverflowQuantitiesNeverChangeInventory、replayOfSuccessfulBorrowDoesNotDeductStockAgainEvenIfNoneRemains、excessReturnAndMissingLoanNeverAddStock、successfulReturnReplayDoesNotAddStockAgain。
 * 主要协作组件：ConsumableLoanMapper、ConsumableMapper、ConsumableService、ConsumableLoanService。
 */
package org.example.backend.service;

import org.example.backend.entity.ConsumableEntity;
import org.example.backend.entity.ConsumableLoanEntity;
import org.example.backend.entity.ConsumableStockRecordEntity;
import org.example.backend.mapper.ConsumableMapper;
import org.example.backend.mapper.ConsumableLoanMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsumableLoanServiceTest {
    private final ConsumableLoanMapper loans = mock(ConsumableLoanMapper.class);
    private final ConsumableMapper items = mock(ConsumableMapper.class);
    private final ConsumableService stock = mock(ConsumableService.class);
    private final ConsumableLoanService service = new ConsumableLoanService(loans, items, stock);

    @Test void fractionalZeroAndOverflowQuantitiesNeverChangeInventory() {
        for (String quantity : new String[]{"0", "-1", "1.5", "2147483648"}) {
            assertThrows(IllegalArgumentException.class, () -> service.borrow(request(quantity, UUID.randomUUID().toString()), "admin"));
        }
        verifyNoInteractions(stock, items, loans);
    }
    @Test void replayOfSuccessfulBorrowDoesNotDeductStockAgainEvenIfNoneRemains() {
        String key = UUID.randomUUID().toString();
        ConsumableEntity item = new ConsumableEntity(); item.setId(1L); item.setStock(0);
        when(items.getConsumableForUpdate(1L)).thenReturn(item);
        ConsumableLoanEntity saved = loan(); saved.setRequestKey(key); saved.setBorrower("张老师");
        saved.setPurpose("教学"); saved.setDueDate(LocalDate.now().toString()); saved.setOperator("admin"); saved.setContact(""); saved.setRemark("");
        when(loans.byRequest(key)).thenReturn(saved);
        assertSame(saved, service.borrow(request("5", key), "admin"));
        verifyNoInteractions(stock);
        verify(loans, never()).insert(any());
    }
    @Test void excessReturnAndMissingLoanNeverAddStock() {
        when(loans.lock(7L)).thenReturn(loan());
        assertThrows(IllegalArgumentException.class, () -> service.returnItems(7L, new ConsumableLoanService.ReturnRequest(new BigDecimal("6"), "", UUID.randomUUID().toString()), "admin"));
        assertThrows(ResponseStatusException.class, () -> service.returnItems(8L, new ConsumableLoanService.ReturnRequest(BigDecimal.ONE, "", UUID.randomUUID().toString()), "admin"));
        verifyNoInteractions(stock);
        verify(loans, never()).addReturned(any(), anyInt());
    }
    @Test void successfulReturnReplayDoesNotAddStockAgain() {
        String key = UUID.randomUUID().toString();
        ConsumableLoanEntity completed = loan(); completed.setRemainingQuantity(0);
        when(loans.lock(7L)).thenReturn(completed); when(loans.get(7L)).thenReturn(completed);
        ConsumableStockRecordEntity previous = new ConsumableStockRecordEntity();
        previous.setQuantity(5); previous.setConsumableId(1L); previous.setOperator("admin"); previous.setRemark("");
        when(items.getStockRecordBySource("借用归还#7:" + key)).thenReturn(previous);
        assertSame(completed, service.returnItems(7L, new ConsumableLoanService.ReturnRequest(new BigDecimal("5"), "", key), "admin"));
        verifyNoInteractions(stock);
        verify(loans, never()).addReturned(any(), anyInt());
    }
    private ConsumableLoanService.BorrowRequest request(String quantity, String key) {
        return new ConsumableLoanService.BorrowRequest(1L, "张老师", "", new BigDecimal(quantity), "教学", LocalDate.now(), "", key);
    }
    private ConsumableLoanEntity loan() {
        ConsumableLoanEntity loan = new ConsumableLoanEntity(); loan.setId(7L); loan.setConsumableId(1L); loan.setQuantity(5); loan.setRemainingQuantity(5);
        return loan;
    }
}
