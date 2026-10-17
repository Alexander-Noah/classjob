/*
 * 耗材借还数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、requestKey、consumableId、consumableName、unit、borrower；JSON 别名和类型转换以字段注解为准。
 */
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
