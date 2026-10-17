/*
 * 耗材借还实现模块。
 * 主要协作组件：ConsumableLoanMapper、ConsumableMapper、ConsumableService。
 */
package org.example.backend.service;

import org.example.backend.entity.ConsumableEntity;
import org.example.backend.entity.ConsumableLoanEntity;
import org.example.backend.entity.ConsumableStockRecordEntity;
import org.example.backend.mapper.ConsumableLoanMapper;
import org.example.backend.mapper.ConsumableMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ConsumableLoanService {
    public record BorrowRequest(Long consumableId, String borrower, String contact, BigDecimal quantity,
                                String purpose, LocalDate dueDate, String remark, String requestId) {}
    public record ReturnRequest(BigDecimal quantity, String remark, String requestId) {}
    private final ConsumableLoanMapper loans;
    private final ConsumableMapper consumables;
    private final ConsumableService stockService;

    /**
     * 构造 ConsumableLoanService，初始化当前实例。
     * @param loans 借用参数。
     * @param consumables consumables参数。
     * @param stockService 库存服务参数。
     */
    public ConsumableLoanService(ConsumableLoanMapper loans, ConsumableMapper consumables, ConsumableService stockService) {
        this.loans = loans;
        this.consumables = consumables;
        this.stockService = stockService;
    }

    /**
     * 列出当前操作。
     * @return 当前操作得到的记录集合。
     */
    public List<ConsumableLoanEntity> list() { return loans.list(); }
    /**
     * 获取当前操作。
     * @param id 标识参数。
     * @return ConsumableLoanEntity 类型的处理结果。
     */
    public ConsumableLoanEntity get(Long id) {
        ConsumableLoanEntity loan = loans.get(id);
        if (loan == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "借用记录不存在");
        return loan;
    }

    /**
     * 借出当前操作。
     * 涉及：写入数据库；通过 Spring 代理入口应用事务配置。
     * @param request 请求参数。
     * @param operator operator参数。
     * @return ConsumableLoanEntity 类型的处理结果。
     */
    @Transactional
    public ConsumableLoanEntity borrow(BorrowRequest request, String operator) {
        if (request == null || request.consumableId() == null) throw new IllegalArgumentException("请选择耗材");
        String borrower = required(request.borrower(), "借用人", 100);
        String purpose = required(request.purpose(), "借用用途", 200);
        String contact = optional(request.contact(), "联系方式", 100);
        String remark = optional(request.remark(), "备注", 500);
        int quantity = positiveQuantity(request.quantity());
        String key = requestKey(request.requestId());
        if (request.dueDate() == null) throw new IllegalArgumentException("请选择应还日期");
        String date = request.dueDate().toString();
        ConsumableEntity item = consumables.getConsumableForUpdate(request.consumableId());
        if (item == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "耗材不存在或已删除");
        // Serializing on the inventory row makes retries safe without deducting stock twice.
        ConsumableLoanEntity existing = loans.byRequest(key);
        if (existing != null) {
            if (!Objects.equals(existing.getConsumableId(), request.consumableId()) || existing.getQuantity() != quantity
                    || !Objects.equals(existing.getBorrower(), borrower) || !Objects.equals(existing.getPurpose(), purpose)
                    || !Objects.equals(existing.getDueDate(), date) || !Objects.equals(existing.getOperator(), operator)
                    || !Objects.equals(existing.getContact(), contact) || !Objects.equals(existing.getRemark(), remark)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "该提交编号已用于其他借用，请重新登记");
            }
            return existing;
        }
        if (request.dueDate().isBefore(LocalDate.now())) throw new IllegalArgumentException("应还日期不能早于今天");
        if (item.getStock() == null || item.getStock() < quantity) throw new IllegalArgumentException("借用数量不能超过当前可用库存");
        ConsumableLoanEntity loan = new ConsumableLoanEntity();
        loan.setRequestKey(key); loan.setConsumableId(item.getId()); loan.setConsumableName(item.getName());
        loan.setUnit(item.getUnit()); loan.setBorrower(borrower); loan.setContact(contact); loan.setQuantity(quantity);
        loan.setPurpose(purpose); loan.setDueDate(date); loan.setRemark(remark); loan.setOperator(required(operator, "经办人", 100));
        loans.insert(loan);
        ConsumableStockRecordEntity movement = movement("出库", quantity, operator, "借用#" + loan.getId(), "耗材借用：" + purpose, remark);
        stockService.recordConsumableMovement(item.getId(), movement);
        return get(loan.getId());
    }

    /**
     * 归还条目集合。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param id 标识参数。
     * @param request 请求参数。
     * @param operator operator参数。
     * @return ConsumableLoanEntity 类型的处理结果。
     */
    @Transactional
    public ConsumableLoanEntity returnItems(Long id, ReturnRequest request, String operator) {
        if (request == null) throw new IllegalArgumentException("请填写归还信息");
        int quantity = positiveQuantity(request.quantity());
        String remark = optional(request.remark(), "归还备注", 500);
        String source = "借用归还#" + id + ":" + requestKey(request.requestId());
        ConsumableLoanEntity loan = loans.lock(id);
        if (loan == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "借用记录不存在");
        ConsumableStockRecordEntity previous = consumables.getStockRecordBySource(source);
        if (previous != null) {
            if (previous.getQuantity() != quantity || !Objects.equals(previous.getConsumableId(), loan.getConsumableId())
                    || !Objects.equals(previous.getOperator(), operator) || !Objects.equals(previous.getRemark(), remark)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "该提交编号已用于其他归还，请重新操作");
            }
            return get(id);
        }
        if (quantity > loan.getRemainingQuantity()) throw new IllegalArgumentException("归还数量不能超过尚未归还的数量");
        if (loans.addReturned(id, quantity) != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "归还记录已变化，请刷新重试");
        stockService.recordConsumableMovement(loan.getConsumableId(), movement("入库", quantity,
                required(operator, "经办人", 100), source, "耗材归还（借用人：" + loan.getBorrower() + "）", remark));
        return get(id);
    }

    /**
     * movement的处理逻辑。
     * @param type 类型参数。
     * @param quantity quantity参数。
     * @param operator operator参数。
     * @param source 来源参数。
     * @param reason 原因参数。
     * @param remark remark参数。
     * @return ConsumableStockRecordEntity 类型的处理结果。
     */
    private ConsumableStockRecordEntity movement(String type, int quantity, String operator, String source, String reason, String remark) {
        ConsumableStockRecordEntity movement = new ConsumableStockRecordEntity();
        movement.setType(type); movement.setQuantity(quantity); movement.setOperator(operator);
        movement.setSource(source); movement.setReason(reason); movement.setRemark(remark);
        return movement;
    }
    /**
     * positiveQuantity的处理逻辑。
     * @param value 值参数。
     * @return int 类型的处理结果。
     */
    private int positiveQuantity(BigDecimal value) {
        try { int quantity = value == null ? 0 : value.intValueExact(); if (quantity > 0) return quantity; }
        catch (ArithmeticException ignored) {}
        throw new IllegalArgumentException("数量必须为大于 0 的整数");
    }
    /**
     * 请求键的处理逻辑。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String requestKey(String value) {
        try { String key = UUID.fromString(value).toString(); if (!key.equalsIgnoreCase(value)) throw new IllegalArgumentException(); return key; }
        catch (RuntimeException error) { throw new IllegalArgumentException("提交编号无效，请关闭弹窗后重试"); }
    }
    /**
     * required的处理逻辑。
     * @param value 值参数。
     * @param label 标签参数。
     * @param max max参数。
     * @return String 类型的处理结果。
     */
    private String required(String value, String label, int max) {
        String text = optional(value, label, max);
        if (text.isEmpty()) throw new IllegalArgumentException("请填写" + label);
        return text;
    }
    /**
     * optional的处理逻辑。
     * @param value 值参数。
     * @param label 标签参数。
     * @param max max参数。
     * @return String 类型的处理结果。
     */
    private String optional(String value, String label, int max) {
        String text = value == null ? "" : value.trim();
        if (text.length() > max) throw new IllegalArgumentException(label + "不能超过" + max + "字");
        return text;
    }
}
