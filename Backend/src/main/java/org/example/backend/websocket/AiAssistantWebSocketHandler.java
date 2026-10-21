/*
 * AI 助手实现模块。
 * 主要协作组件：AiAssistantService、SpeechRecognitionService、ObjectMapper。
 */
package org.example.backend.websocket;

import lombok.extern.slf4j.Slf4j;
import org.example.backend.service.AiAssistantService;
import org.example.backend.service.SpeechRecognitionService;
import org.example.backend.service.RolePageAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.vosk.Recognizer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class AiAssistantWebSocketHandler extends AbstractWebSocketHandler implements SubProtocolCapable {
    private static final TypeReference<Map<String, Object>> MESSAGE_TYPE = new TypeReference<>() {
    };

    private final AiAssistantService aiAssistantService;
    private final SpeechRecognitionService speechRecognitionService;
    private final ObjectMapper objectMapper;
    private RolePageAccessService pageAccess;

    @Autowired
    public void configureRolePageAccess(RolePageAccessService pageAccess) {
        this.pageAccess = pageAccess;
    }

    private boolean hasPageAccess(WebSocketSession session) throws Exception {
        if (pageAccess == null || pageAccess.isApiAllowed("GET", "/ai-assistant/ws", asString(session.getAttributes().get("authRole")))) return true;
        closeRecognizer(session);
        session.close(CloseStatus.POLICY_VIOLATION);
        return false;
    }

    /**
     * 获取Sub协议。
     * @return 当前操作得到的记录集合。
     */
    @Override
    public List<String> getSubProtocols() {
        // Negotiate a fixed protocol; the JWT is only consumed by the handshake interceptor.
        return List.of("smart-lab.ai.v1");
    }

    /**
     * 构造 AiAssistantWebSocketHandler，初始化当前实例。
     * @param aiAssistantService AIAssistant服务参数。
     * @param speechRecognitionService 语音Recognition服务参数。
     * @param objectMapper object数据映射器参数。
     */
    public AiAssistantWebSocketHandler(
            AiAssistantService aiAssistantService,
            SpeechRecognitionService speechRecognitionService,
            ObjectMapper objectMapper
    ) {
        this.aiAssistantService = aiAssistantService;
        this.speechRecognitionService = speechRecognitionService;
        this.objectMapper = objectMapper;
    }

    /**
     * 在执行后处理连接Established。
     * @param session 会话参数。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.getAttributes().put("sessionId", UUID.randomUUID().toString());
        log.info("AI assistant websocket connected session={}", session.getId());
    }

    /**
     * 处理文本消息。
     * @param session 会话参数。
     * @param message 消息参数。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (!hasPageAccess(session)) return;
        try { org.example.backend.security.RequestInputValidation.websocketText(message.getPayload()); }
        catch (IllegalArgumentException e) {
            sendJson(session, Map.of("type", "error", "text", e.getMessage()));
            return;
        }
        Map<String, Object> payload = readMessage(message.getPayload());
        String type = asString(payload.get("type"));
        String state = asString(payload.get("state"));

        if ("hello".equals(type)) {
            sendJson(session, Map.of(
                    "type", "hello",
                    "session_id", session.getAttributes().get("sessionId")
            ));
            return;
        }

        if ("abort".equals(type)) {
            closeRecognizer(session);
            sendJson(session, Map.of("type", "tts", "state", "stop"));
            return;
        }

        if ("listen".equals(type) && "start".equals(state)) {
            startSpeechRecognition(session);
            return;
        }

        if ("listen".equals(type) && "stop".equals(state)) {
            stopSpeechRecognition(session);
            return;
        }

        if ("listen".equals(type) && "detect".equals(state)) {
            String question = asString(payload.get("text"));
            sendAnswer(session, question, asString(session.getAttributes().get("sessionId")), asImageList(payload.get("images")));
        }
    }

    /**
     * 处理Binary消息。
     * @param session 会话参数。
     * @param message 消息参数。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        if (!hasPageAccess(session)) return;
        Object recognizerValue = session.getAttributes().get("recognizer");
        if (!(recognizerValue instanceof Recognizer recognizer)) {
            return;
        }

        ByteBuffer payload = message.getPayload();
        byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);
        if (recognizer.acceptWaveForm(bytes, bytes.length)) {
            appendTranscriptSegment(session, speechRecognitionService.getResultText(recognizer));
        }
    }

    /**
     * 在执行后处理连接Closed。
     * @param session 会话参数。
     * @param status 状态参数。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        closeRecognizer(session);
        log.info("AI assistant websocket closed session={} status={}", session.getId(), status);
    }

    /**
     * 启动语音Recognition。
     * @param session 会话参数。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private void startSpeechRecognition(WebSocketSession session) throws Exception {
        closeRecognizer(session);
        if (!speechRecognitionService.isAvailable()) {
            sendJson(session, Map.of(
                    "type", "stt",
                    "state", "error",
                    "text", "语音识别服务暂不可用，请检查后端 Vosk 模型路径配置"
            ));
            return;
        }
        session.getAttributes().put("recognizer", speechRecognitionService.createRecognizer());
        session.getAttributes().put("transcriptSegments", new ArrayList<String>());
        sendJson(session, Map.of("type", "listen", "state", "start"));
    }

    /**
     * 停止语音Recognition。
     * @param session 会话参数。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private void stopSpeechRecognition(WebSocketSession session) throws Exception {
        Object recognizerValue = session.getAttributes().remove("recognizer");
        @SuppressWarnings("unchecked")
        List<String> transcriptSegments = (List<String>) session.getAttributes().remove("transcriptSegments");
        if (!(recognizerValue instanceof Recognizer recognizer)) {
            sendJson(session, Map.of("type", "stt", "state", "empty", "text", ""));
            return;
        }

        try (recognizer) {
            String partialTranscript = speechRecognitionService.getPartialText(recognizer);
            appendTranscriptSegment(transcriptSegments, speechRecognitionService.getFinalText(recognizer));
            String transcript = joinTranscript(transcriptSegments);
            if (transcript.isBlank()) {
                transcript = partialTranscript;
            }
            sendJson(session, Map.of("type", "stt", "state", "final", "text", transcript));
            if (transcript.isBlank()) {
                sendJson(session, Map.of("type", "text", "text", "未识别到有效语音内容，请重新输入。"));
                return;
            }
        }
    }

    /**
     * 关闭Recognizer。
     * @param session 会话参数。
     */
    private void closeRecognizer(WebSocketSession session) {
        Object recognizerValue = session.getAttributes().remove("recognizer");
        if (recognizerValue instanceof Recognizer recognizer) {
            recognizer.close();
        }
        session.getAttributes().remove("transcriptSegments");
    }

    /**
     * 发送Answer。
     * @param session 会话参数。
     * @param question question参数。
     * @param agentSessionId agent会话标识参数。
     * @param images images参数。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private void sendAnswer(
            WebSocketSession session,
            String question,
            String agentSessionId,
            List<Map<String, Object>> images
    ) throws Exception {
        String answer = aiAssistantService.generateAnswer(question, agentSessionId, images);
        sendJson(session, Map.of("type", "tts", "state", "start"));
        sendJson(session, Map.of("type", "tts", "state", "sentence_start", "text", answer));
        sendJson(session, Map.of("type", "tts", "state", "stop"));
    }

    /**
     * 读取消息。
     * @param payload 载荷参数。
     * @return 包含 type、listen、state、detect、text 等字段的结果对象。
     */
    private Map<String, Object> readMessage(String payload) {
        try {
            return objectMapper.readValue(payload, MESSAGE_TYPE);
        } catch (Exception exception) {
            return Map.of(
                    "type", "listen",
                    "state", "detect",
                    "text", payload
            );
        }
    }

    /**
     * 发送JSON。
     * @param session 会话参数。
     * @param payload 载荷参数。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private void sendJson(WebSocketSession session, Map<String, Object> payload) throws Exception {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        }
    }

    /**
     * as字符串的处理逻辑。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * asImage列表的处理逻辑。
     * @param value 值参数。
     * @return 当前操作得到的记录集合。
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asImageList(Object value) {
        if (!(value instanceof List<?> items)) {
            return List.of();
        }

        List<Map<String, Object>> images = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?> map) {
                images.add((Map<String, Object>) map);
            }
        }
        return images;
    }

    /**
     * 追加TranscriptSegment。
     * @param session 会话参数。
     * @param text 文本参数。
     */
    @SuppressWarnings("unchecked")
    private void appendTranscriptSegment(WebSocketSession session, String text) {
        List<String> transcriptSegments = (List<String>) session.getAttributes().get("transcriptSegments");
        appendTranscriptSegment(transcriptSegments, text);
    }

    /**
     * 追加TranscriptSegment。
     * @param transcriptSegments transcriptSegments参数。
     * @param text 文本参数。
     */
    private void appendTranscriptSegment(List<String> transcriptSegments, String text) {
        if (transcriptSegments == null || text == null || text.isBlank()) {
            return;
        }
        transcriptSegments.add(normalizeTranscript(text));
    }

    /**
     * 连接Transcript。
     * @param transcriptSegments transcriptSegments参数。
     * @return String 类型的处理结果。
     */
    private String joinTranscript(List<String> transcriptSegments) {
        if (transcriptSegments == null || transcriptSegments.isEmpty()) {
            return "";
        }
        return normalizeTranscript(String.join(" ", transcriptSegments));
    }

    /**
     * 归一化Transcript。
     * @param text 文本参数。
     * @return String 类型的处理结果。
     */
    private String normalizeTranscript(String text) {
        return text
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("(?<=[\\p{IsHan}])\\s+(?=[\\p{IsHan}])", "");
    }
}
