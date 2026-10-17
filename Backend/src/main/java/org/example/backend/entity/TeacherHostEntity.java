/*
 * 教师主机数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、labId、teacherDeviceId、hostIp、port、status；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeacherHostEntity {
    private Long id;
    /** 教师主机所属实验室；心跳和查询均以此限定数据范围。 */
    private Long labId;
    /** 教师设备的稳定标识，不使用临时 IP 作为设备主键。 */
    private String teacherDeviceId;
    /** 学生端可连接的主机地址，由上报方提供，不是 Web 管理端地址。 */
    private String hostIp;
    /** 教师服务监听端口，单位为 TCP/UDP 端口号，服务层要求为正数。 */
    private Integer port;
    private String status;
    /** 连接凭据，诊断日志应隐藏完整值；字段本身不执行认证。 */
    private String token;
    /** 后端接收心跳时写入的本地时间，供过期在线状态判断使用。 */
    private LocalDateTime lastHeartbeatTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
