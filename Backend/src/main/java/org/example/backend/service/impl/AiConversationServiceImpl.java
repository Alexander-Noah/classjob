package org.example.backend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.entity.AiConversationEntity;
import org.example.backend.mapper.AiConversationMapper;
import org.example.backend.service.AiConversationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 按用户隔离 AI 会话历史，消息序列使用 JSON 保存，并限制每个用户保留的会话数。 */
@Service
public class AiConversationServiceImpl implements AiConversationService {
    private static final int MAX_HISTORY_COUNT = 30;
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_PREVIEW_LENGTH = 300;
    private static final TypeReference<List<Map<String, Object>>> MESSAGE_LIST_TYPE = new TypeReference<>() {
    };

    private final AiConversationMapper conversationMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造 AiConversationServiceImpl，初始化当前实例。
     * @param conversationMapper 会话数据映射器参数。
     */
    public AiConversationServiceImpl(AiConversationMapper conversationMapper) {
        this.conversationMapper = conversationMapper;
    }

    /**
     * 列出会话。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param userId 用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listConversations(Integer userId) {
        requireUserId(userId);
        return conversationMapper.listByUser(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 保存会话。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param userId 用户标识参数。
     * @param conversations 会话参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    @Transactional
    public List<Map<String, Object>> saveConversations(Integer userId, List<Map<String, Object>> conversations) {
        requireUserId(userId);
        // 空同步不会删除服务端历史；单个会话删除走显式的用户范围删除接口。
        if (conversations == null || conversations.isEmpty()) {
            return listConversations(userId);
        }

        conversations.stream()
                .limit(MAX_HISTORY_COUNT)
                .map(/** 将当前条目转换为目标结果。 入参：conversation。 */ (conversation) -> toEntity(userId, conversation))
                .forEach(conversationMapper::upsertConversation);
        conversationMapper.trimUserConversations(userId, MAX_HISTORY_COUNT);
        return listConversations(userId);
    }

    /**
     * 删除会话。
     * 涉及：写入数据库；通过 Spring 代理入口应用事务配置。
     * @param userId 用户标识参数。
     * @param conversationId 会话标识参数。
     */
    @Override
    @Transactional
    public void deleteConversation(Integer userId, String conversationId) {
        requireUserId(userId);
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("会话 ID 不能为空");
        }
        // 必须同时匹配用户和会话 ID，防止跨用户删除同名会话。
        conversationMapper.deleteByUserAndConversationId(userId, conversationId);
    }

    /**
     * 要求提供用户标识。
     * @param userId 用户标识参数。
     */
    private void requireUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("登录状态无效，请重新登录");
        }
    }

    /**
     * 转换为Entity。
     * @param userId 用户标识参数。
     * @param payload 载荷参数。
     * @return AiConversationEntity 类型的处理结果。
     */
    private AiConversationEntity toEntity(Integer userId, Map<String, Object> payload) {
        Map<String, Object> conversation = payload == null ? Map.of() : payload;
        String conversationId = trimToNull(stringValue(conversation.get("id")));
        if (conversationId == null) {
            conversationId = "conversation-" + System.currentTimeMillis();
        }

        Object messages = conversation.get("messages");
        List<?> messageList = messages instanceof List<?> list ? list : List.of();

        AiConversationEntity entity = new AiConversationEntity();
        entity.setUserId(userId);
        entity.setConversationId(limit(conversationId, 120));
        entity.setTitle(limit(firstNonBlank(stringValue(conversation.get("title")), "新的咨询"), MAX_TITLE_LENGTH));
        entity.setPreview(limit(firstNonBlank(stringValue(conversation.get("preview")), "暂无对话内容"), MAX_PREVIEW_LENGTH));
        entity.setUpdatedAtMillis(numberValue(conversation.get("updatedAt"), System.currentTimeMillis()));
        entity.setMessagesJson(writeMessages(messageList));
        return entity;
    }

    /**
     * 转换为响应。
     * @param entity entity参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, Object> toResponse(AiConversationEntity entity) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", entity.getConversationId());
        response.put("title", entity.getTitle());
        response.put("preview", entity.getPreview());
        response.put("updatedAt", entity.getUpdatedAtMillis());
        response.put("messages", readMessages(entity.getMessagesJson()));
        return response;
    }

    /**
     * 写入消息。
     * @param messages 消息参数。
     * @return String 类型的处理结果。
     */
    private String writeMessages(List<?> messages) {
        try {
            return objectMapper.writeValueAsString(messages == null ? List.of() : messages);
        } catch (Exception exception) {
            return "[]";
        }
    }

    /**
     * 读取消息。
     * @param messagesJson 消息JSON参数。
     * @return 当前操作得到的记录集合。
     */
    private List<Map<String, Object>> readMessages(String messagesJson) {
        if (messagesJson == null || messagesJson.isBlank()) {
            return List.of();
        }

        try {
            List<Map<String, Object>> messages = objectMapper.readValue(messagesJson, MESSAGE_LIST_TYPE);
            return messages == null ? List.of() : messages;
        } catch (Exception exception) {
            return List.of();
        }
    }

    /**
     * 将输入转换为字符串值。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 裁剪目标Null。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * first非空白的处理逻辑。
     * @return String 类型的处理结果。
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return "";
    }

    /**
     * 数值值的处理逻辑。
     * @param value 值参数。
     * @param fallback fallback参数。
     * @return long 类型的处理结果。
     */
    private long numberValue(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception exception) {
            return fallback;
        }
    }

    /**
     * 上限的处理逻辑。
     * @param value 值参数。
     * @param maxLength max长度参数。
     * @return String 类型的处理结果。
     */
    private String limit(String value, int maxLength) {
        String text = firstNonBlank(value);
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
