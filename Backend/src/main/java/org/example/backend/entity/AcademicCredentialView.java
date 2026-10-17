/*
 * 教务凭据数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：credentialKey、usernameMasked、configured、usable、status、message；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

@Data
public class AcademicCredentialView {
    private String credentialKey;
    /** 仅返回脱敏账号；本视图不定义明文密码或密文字段。 */
    private String usernameMasked;
    /** 是否存在保存的凭据，不代表当前密钥仍可解密。 */
    private Boolean configured;
    /** 服务端是否确认凭据可用；调用方不应仅凭 configured 启动采集。 */
    private Boolean usable;
    private String status;
    private String message;
    private String updatedAt;
}
