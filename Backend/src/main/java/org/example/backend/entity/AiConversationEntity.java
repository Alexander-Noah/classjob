/*
 * AI 会话历史数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、userId、conversationId、title、preview、updatedAtMillis；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiConversationEntity {
    private Long id;
    private Integer userId;
    private String conversationId;
    private String title;
    private String preview;
    private Long updatedAtMillis;
    private String messagesJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
