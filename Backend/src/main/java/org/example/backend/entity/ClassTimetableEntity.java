/*
 * 全校课表采集数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、semester、rowClassName、className、weekday、sectionCode；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassTimetableEntity {
    private Long id;
    private String semester;
    private String rowClassName;
    private String className;
    private String weekday;
    private String sectionCode;
    private String sectionText;
    private Integer startSection;
    private Integer endSection;
    private String courseName;
    private String teacher;
    private String weekRaw;
    private String weekText;
    private String weekExpanded;
    private String classroom;
    private LocalDateTime createTime;
}
