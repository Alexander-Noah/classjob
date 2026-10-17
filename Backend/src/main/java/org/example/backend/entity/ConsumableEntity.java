/*
 * 耗材库存数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、name、category、stock、unit、location；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsumableEntity {
    private Long id;
    private String name;
    private String category;
    private Integer stock;
    private String unit;
    private String location;
    private Integer warnThreshold;
    private String status;
    private String tagType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
