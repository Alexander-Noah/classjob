/*
 * 实验室数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、labCode、labName、building、floor、roomNo；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LabEntity {
    private Integer id;
    @JsonAlias("code")
    private String labCode;
    @JsonAlias("name")
    private String labName;
    private String building;
    private String floor;
    @JsonAlias("room")
    private String roomNo;
    @JsonAlias("type")
    private String labType;
    private Integer capacity;
    private Integer managerUserId;
    @JsonAlias("manager")
    private String managerName;
    @JsonAlias("status")
    private String openStatus;
    @JsonAlias("devices")
    private Integer deviceCount;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
