/*
 * 耗材库存数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、consumableId、consumableName、category、type、quantity；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsumableStockRecordEntity {
    private Long id;
    private Long consumableId;
    private String consumableName;
    private String category;
    private String type;
    private Integer quantity;
    private String unit;
    private Integer beforeStock;
    private Integer afterStock;
    private String operator;
    private String source;
    private String reason;
    private String remark;
    private String time;
    private LocalDateTime createdAt;
}
