/*
 * 用户数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、username、password、realName、phone、email；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserEntity {
    private Integer id;
    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String realName;
    private String phone;
    private String email;
    private String department;
    private String avatarUrl;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String roleCode;
    private String roleName;
    private String permissions;
}
