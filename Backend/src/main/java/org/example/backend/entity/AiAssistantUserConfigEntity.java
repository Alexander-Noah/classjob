/*
 * AI 助手数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、userId、enabled、baseUrl、apiKey、model；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiAssistantUserConfigEntity {
    private Long id;
    private Integer userId;
    private Boolean enabled;
    private String baseUrl;
    private String apiKey;
    private String model;
    private String visionModel;
    private String systemPrompt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
