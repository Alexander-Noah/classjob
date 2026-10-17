/*
 * 通义千问实现模块。
 * 主要协作组件：ObjectMapper。
 */
package org.example.backend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.backend.service.TongyiAgentService;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class TongyiAgentServiceImpl implements TongyiAgentService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final Path CONFIG_PATH = Path.of("data", "tongyi-api-config.json");
    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final String DEFAULT_MODEL = "qwen-plus";
    private static final String DEFAULT_VISION_MODEL = "qwen-vl-plus";

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final Environment environment;
    private Map<String, Object> fileConfig;

    /**
     * 构造 TongyiAgentServiceImpl，初始化当前实例。
     * @param objectMapper object数据映射器参数。
     * @param environment 环境参数。
     */
    public TongyiAgentServiceImpl(ObjectMapper objectMapper, Environment environment) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
        this.environment = environment;
        this.fileConfig = loadFileConfig();
    }

    /**
     * 获取配置。
     * @return 当前操作返回的键值结果。
     */
    @Override
    public synchronized Map<String, Object> getConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("configured", isConfigured());
        data.put("base_url", getBaseUrl());
        data.put("model", getModel());
        data.put("vision_model", getVisionModel());
        data.put("api_key_configured", !getApiKey().isBlank());
        return data;
    }

    /**
     * 更新配置。
     * @param payload 载荷参数。
     * @return 当前操作返回的键值结果。
     */
    @Override
    public synchronized Map<String, Object> updateConfig(Map<String, Object> payload) {
        putIfPresent(payload, "api_key");
        putIfPresent(payload, "base_url");
        putIfPresent(payload, "model");
        putIfPresent(payload, "vision_model");
        putIfPresent(payload, "enabled");
        saveFileConfig();
        return getConfig();
    }

    /**
     * 判断是否为Configured。
     * @return 当前判断的布尔结果。
     */
    @Override
    public synchronized boolean isConfigured() {
        return getEnabled() && !getApiKey().isBlank();
    }

    /**
     * callAgent的处理逻辑。
     * @param question question参数。
     * @param sessionId 会话标识参数。
     * @return String 类型的处理结果。
     */
    @Override
    public String callAgent(String question, String sessionId) {
        return callAgent(question, sessionId, List.of());
    }

    /**
     * callAgent的处理逻辑。
     * @param question question参数。
     * @param sessionId 会话标识参数。
     * @param images images参数。
     * @return String 类型的处理结果。
     */
    @Override
    public String callAgent(String question, String sessionId, List<Map<String, Object>> images) {
        if (!isConfigured()) {
            throw new IllegalStateException("通义 API Key 尚未配置");
        }

        List<Map<String, Object>> validImages = normalizeImages(images);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", validImages.isEmpty() ? getModel() : getVisionModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", "你是实验室协同管控平台的AI助手，回答应专业、简洁、可执行。"),
                Map.of("role", "user", "content", buildUserContent(question, validImages))
        ));
        body.put("temperature", 0.7);

        String responseBody = restClient.post()
                .uri(trimTrailingSlash(getBaseUrl()) + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("通义接口返回内容为空");
        }

        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, MAP_TYPE);
            return extractText(response);
        } catch (Exception exception) {
            log.warn("解析通义接口响应失败", exception);
            throw new IllegalStateException("解析通义接口响应失败", exception);
        }
    }

    /**
     * 构建用户内容。
     * @param question question参数。
     * @param images images参数。
     * @return Object 类型的处理结果。
     */
    private Object buildUserContent(String question, List<Map<String, Object>> images) {
        String text = question == null || question.isBlank() ? "请分析这张图片。" : question;

        if (images.isEmpty()) {
            return text;
        }

        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", text));

        for (Map<String, Object> image : images) {
            String dataUrl = asString(image.get("dataUrl"), "");
            if (!dataUrl.isBlank()) {
                content.add(Map.of(
                        "type", "image_url",
                        "image_url", Map.of("url", dataUrl)
                ));
            }
        }

        return content;
    }

    /**
     * 归一化Images。
     * @param images images参数。
     * @return 当前操作得到的记录集合。
     */
    private List<Map<String, Object>> normalizeImages(List<Map<String, Object>> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        return images.stream()
                .filter(/** 判断当前条目是否符合筛选条件。 入参：image。 */ image -> image != null && asString(image.get("dataUrl"), "").startsWith("data:image/"))
                .limit(3)
                .toList();
    }

    /**
     * 提取文本。
     * @param response 响应参数。
     * @return String 类型的处理结果。
     */
    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        Object choices = response.get("choices");
        if (choices instanceof List<?> choiceList && !choiceList.isEmpty() && choiceList.get(0) instanceof Map<?, ?> choice) {
            Object message = ((Map<String, Object>) choice).get("message");
            if (message instanceof Map<?, ?> messageMap) {
                Object content = ((Map<String, Object>) messageMap).get("content");
                if (content != null && !String.valueOf(content).isBlank()) {
                    return String.valueOf(content);
                }
            }
        }
        throw new IllegalStateException("通义接口响应中没有可展示的文本内容");
    }

    /**
     * 加载文件配置。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, Object> loadFileConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                return objectMapper.readValue(Files.readString(CONFIG_PATH), MAP_TYPE);
            }
        } catch (Exception exception) {
            log.warn("无法加载通义 API 配置文件", exception);
        }

        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("enabled", true);
        defaults.put("base_url", DEFAULT_BASE_URL);
        defaults.put("model", DEFAULT_MODEL);
        defaults.put("vision_model", DEFAULT_VISION_MODEL);
        defaults.put("api_key", "");
        return defaults;
    }

    /**
     * putIfPresent的处理逻辑。
     * @param payload 载荷参数。
     * @param key 键参数。
     */
    private void putIfPresent(Map<String, Object> payload, String key) {
        if (!payload.containsKey(key)) {
            return;
        }
        Object value = payload.get(key);
        if ("api_key".equals(key) && "******".equals(String.valueOf(value))) {
            return;
        }
        fileConfig.put(key, value);
    }

    /**
     * 保存文件配置。
     * 涉及：写入文件。
     */
    private void saveFileConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, objectMapper.writeValueAsString(fileConfig));
        } catch (IOException exception) {
            throw new IllegalStateException("保存通义 API 配置失败", exception);
        }
    }

    /**
     * 获取已启用。
     * @return 当前判断的布尔结果。
     */
    private boolean getEnabled() {
        String value = getProperty("tongyi.agent.enabled", "TONGYI_AGENT_ENABLED", asString(fileConfig.get("enabled"), "true"));
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    /**
     * 获取基础URL。
     * @return String 类型的处理结果。
     */
    private String getBaseUrl() {
        return getProperty("tongyi.agent.base-url", "TONGYI_AGENT_BASE_URL", asString(fileConfig.get("base_url"), DEFAULT_BASE_URL));
    }

    /**
     * 获取模型。
     * @return String 类型的处理结果。
     */
    private String getModel() {
        return getProperty("tongyi.agent.model", "TONGYI_MODEL", asString(fileConfig.get("model"), DEFAULT_MODEL));
    }

    /**
     * 获取Vision模型。
     * @return String 类型的处理结果。
     */
    private String getVisionModel() {
        return getProperty(
                "tongyi.agent.vision-model",
                "TONGYI_VISION_MODEL",
                asString(fileConfig.get("vision_model"), DEFAULT_VISION_MODEL)
        );
    }

    /**
     * 获取API键。
     * @return String 类型的处理结果。
     */
    private String getApiKey() {
        String value = environment.getProperty("tongyi.agent.api-key");
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getenv("TONGYI_API_KEY");
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getenv("DASHSCOPE_API_KEY");
        if (value != null && !value.isBlank()) {
            return value;
        }
        return asString(fileConfig.get("api_key"), "");
    }

    /**
     * 获取Property。
     * @param propertyName property名称参数。
     * @param envName env名称参数。
     * @param defaultValue 默认值参数。
     * @return String 类型的处理结果。
     */
    private String getProperty(String propertyName, String envName, String defaultValue) {
        String value = environment.getProperty(propertyName);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getenv(envName);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 裁剪TrailingSlash。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        return value.replaceAll("/+$", "");
    }

    /**
     * as字符串的处理逻辑。
     * @param value 值参数。
     * @param defaultValue 默认值参数。
     * @return String 类型的处理结果。
     */
    private String asString(Object value, String defaultValue) {
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return String.valueOf(value);
    }
}
