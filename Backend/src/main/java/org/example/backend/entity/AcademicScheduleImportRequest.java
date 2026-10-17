/*
 * 教务课表集成数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：username、password、randomCode、credentialKey、saveCredential、useSavedCredential；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

@Data
public class AcademicScheduleImportRequest {
    private String username;
    private String password;
    private String randomCode;
    private String credentialKey;
    private Boolean saveCredential;
    private Boolean useSavedCredential;
    private String cookie;
    private String html;
    private String baseUrl;
    private String schedulePath;
    private String term;
    private String weekLabel;
    private String teacherName;
    private Integer teacherUserId;
    private String className;
    private String labType;
    private Boolean importToCourseEnvironments;
}
