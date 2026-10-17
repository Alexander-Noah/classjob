/*
 * 维护记录数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、recordNo、labId、deviceId、lab、device；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

@Data
public class MaintenanceRecord {
    private String id;
    private String recordNo;
    private Long labId;
    private Long deviceId;
    private String lab;
    private String device;
    private String maintenanceType;
    private String maintenanceDate;
    private String maintainer;
    private String content;
    private String result;
    private String remark;
    private String updatedAt;
}
