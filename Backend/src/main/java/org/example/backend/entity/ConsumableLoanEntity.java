package org.example.backend.entity;

import lombok.Data;

@Data
public class ConsumableLoanEntity {
    private Long id;
    private String requestKey;
    private Long consumableId;
    private String consumableName;
    private String unit;
    private String borrower;
    private String contact;
    private Integer quantity;
    private Integer returnedQuantity;
    private Integer remainingQuantity;
    private String purpose;
    private String dueDate;
    private String remark;
    private String operator;
    private String borrowedAt;
    private String lastReturnedAt;
    private String status;
}
