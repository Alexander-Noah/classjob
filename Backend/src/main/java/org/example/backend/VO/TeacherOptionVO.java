/*
 * 教师工作台数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：teacherName；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.VO;

import lombok.Data;

@Data
public class TeacherOptionVO {
    private String teacherName;
}
