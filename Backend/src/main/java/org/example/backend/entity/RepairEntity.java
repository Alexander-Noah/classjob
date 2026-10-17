/*
 * 报修数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、ticket、reporter、contact、lab、device；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

@Data
public class RepairEntity {
    private String id;
    private String ticket;
    private String reporter;
    private String contact;
    private String lab;
    private String device;
    private String faultType;
    private String priority;
    private String status;
    private String tagType;
    private String assignee;
    private String submittedAt;
    private String deadline;
    private Integer progress;
    private String description;
    private String result;
    private String lastUpdate;
}
