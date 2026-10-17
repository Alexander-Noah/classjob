/*
 * 教务课表集成数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：courseName、teacherName、className、weekLabel、dayLabel、sectionLabel；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

@Data
public class AcademicScheduleCourse {
    private String courseName;
    private String teacherName;
    private String className;
    private String weekLabel;
    private String dayLabel;
    private String sectionLabel;
    private String timeRange;
    private String useTime;
    private String labName;
    private String rawText;
}
