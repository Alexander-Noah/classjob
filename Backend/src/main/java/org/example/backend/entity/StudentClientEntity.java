/*
 * 学生客户端数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、labId、studentDeviceId、hostName、ipAddress、status；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentClientEntity {
    private Long id;
    /** 上下线更新必须提供实验室标识，与 studentDeviceId 一同定位记录。 */
    private Long labId;
    /** 学生终端标识；设备名称与 IP 改变时不应创建新的身份。 */
    private String studentDeviceId;
    private String hostName;
    private String ipAddress;
    private String status;
    /** 协议携带的连接凭据；DTO 不验证其有效性，日志中应脱敏。 */
    private String token;
    /** 由后端登记在线事件时赋值，而不是直接采用客户端时钟。 */
    private LocalDateTime lastHeartbeatTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
