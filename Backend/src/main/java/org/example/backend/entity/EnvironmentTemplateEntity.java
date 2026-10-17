/*
 * 环境模板数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、name、course、os、softwareList、labs；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EnvironmentTemplateEntity {
    private Long id;
    private String name;
    private String course;
    private String os;
    private String softwareList;
    private String labs;
    private String status;
    private String tagType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
