/*
 * 软件环境数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、labId、lab、labName、name、version；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class softwareEntity {
    private Long id;
    private Long labId;
    private String lab;
    private String labName;
    private String name;
    private String version;
    private String type;
    private String license;
    private String status;
    private String tagType;
    private String softwareName;
    private String softwareVersion;
    private String installPath;
    private String licenseInfo;
    private LocalDateTime installTime;
    private String remark;
}
