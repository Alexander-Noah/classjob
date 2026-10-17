/*
 * 设备盘点数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、deviceId、deviceCode、deviceName、inspectorName、inspectedAt；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceInventoryRecordEntity {
    private Long id;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private String inspectorName;
    private String inspectedAt;
    private String resultStatus;
    private Boolean normal;
    private Boolean missing;
    private Boolean damaged;
    private String remark;
    private LocalDateTime createdAt;
}
