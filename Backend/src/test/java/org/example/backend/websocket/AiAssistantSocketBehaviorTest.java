package org.example.backend.websocket;
import org.example.backend.service.AiAssistantService;
import org.example.backend.service.SpeechRecognitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.*;
import org.vosk.Recognizer;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiAssistantSocketBehaviorTest {
    final AiAssistantService ai=mock(AiAssistantService.class);
    final SpeechRecognitionService speech=mock(SpeechRecognitionService.class);
    final WebSocketSession session=mock(WebSocketSession.class);
    final ObjectMapper json=new ObjectMapper();
    final AiAssistantWebSocketHandler handler=new AiAssistantWebSocketHandler(ai,speech,json);
    final Map<String,Object> attributes=new HashMap<>();
    final List<Map<String,Object>> sent=new ArrayList<>();
    @BeforeEach void setup() throws Exception{when(session.getId()).thenReturn("socket-1");when(session.getAttributes()).thenReturn(attributes);when(session.isOpen()).thenReturn(true);doAnswer(call->{TextMessage message=call.getArgument(0);sent.add(json.readValue(message.getPayload(),Map.class));return null;}).when(session).sendMessage(any());handler.afterConnectionEstablished(session);}
    void text(String text) throws Exception{handler.handleMessage(session,new TextMessage(text));}
    @Test void helloNegotiatesFixedProtocolAndReturnsGeneratedSessionIdentity() throws Exception{assertEquals(List.of("smart-lab.ai.v1"),handler.getSubProtocols());assertNotNull(attributes.get("sessionId"));text("{\"type\":\"hello\"}");assertEquals(Map.of("type","hello","session_id",attributes.get("sessionId")),sent.get(0));}
    @Test void detectFiltersNonImageItemsAndEmitsOrderedAnswerFrames() throws Exception{when(ai.generateAnswer(anyString(),anyString(),anyList())).thenReturn("实验室开放");text("{\"type\":\"listen\",\"state\":\"detect\",\"text\":\"状态\",\"images\":[{\"url\":\"data:image/png;base64,a\"},42]}");verify(ai).generateAnswer("状态",attributes.get("sessionId").toString(),List.of(Map.of("url","data:image/png;base64,a")));assertEquals(List.of("start","sentence_start","stop"),sent.stream().map(frame->frame.get("state")).toList());assertEquals("实验室开放",sent.get(1).get("text"));}
    @Test void plainTextFallsBackToQuestionAndClosedSessionsNeverSendFrames() throws Exception{when(ai.generateAnswer(anyString(),anyString(),anyList())).thenReturn("答复");text("普通问题");verify(ai).generateAnswer("普通问题",attributes.get("sessionId").toString(),List.of());sent.clear();when(session.isOpen()).thenReturn(false);text("{\"type\":\"hello\"}");assertEquals(List.of(),sent);}
    @Test void unavailableRecognitionAndStopWithoutRecognizerHaveExplicitStates() throws Exception{text("{\"type\":\"listen\",\"state\":\"start\"}");assertEquals("error",sent.get(0).get("state"));text("{\"type\":\"listen\",\"state\":\"stop\"}");assertEquals("empty",sent.get(1).get("state"));handler.handleMessage(session,new BinaryMessage(new byte[]{1,2}));verifyNoInteractions(ai);}
    @Test void recognitionAccumulatesCompleteSegmentsAndNormalizesChineseSpaces() throws Exception{Recognizer recognizer=mock(Recognizer.class);when(speech.isAvailable()).thenReturn(true);when(speech.createRecognizer()).thenReturn(recognizer);when(recognizer.acceptWaveForm(any(byte[].class),anyInt())).thenReturn(true);when(speech.getResultText(recognizer)).thenReturn(" 实 验 ");when(speech.getFinalText(recognizer)).thenReturn(" 室 开 放 ");when(speech.getPartialText(recognizer)).thenReturn("partial");text("{\"type\":\"listen\",\"state\":\"start\"}");handler.handleMessage(session,new BinaryMessage(new byte[]{1,2,3}));text("{\"type\":\"listen\",\"state\":\"stop\"}");assertEquals("实验室开放",sent.get(1).get("text"));assertEquals("final",sent.get(1).get("state"));verify(recognizer).close();assertFalse(attributes.containsKey("recognizer"));verifyNoInteractions(ai);}
    @Test void recognitionFallsBackToPartialAndEmptySpeechProducesGuidance() throws Exception{Recognizer recognizer=mock(Recognizer.class);when(speech.isAvailable()).thenReturn(true);when(speech.createRecognizer()).thenReturn(recognizer);when(speech.getPartialText(recognizer)).thenReturn("partial");when(speech.getFinalText(recognizer)).thenReturn("");text("{\"type\":\"listen\",\"state\":\"start\"}");text("{\"type\":\"listen\",\"state\":\"stop\"}");assertEquals("partial",sent.get(1).get("text"));when(speech.getPartialText(recognizer)).thenReturn("");sent.clear();text("{\"type\":\"listen\",\"state\":\"start\"}");text("{\"type\":\"listen\",\"state\":\"stop\"}");assertEquals("text",sent.get(2).get("type"));assertTrue(sent.get(2).get("text").toString().contains("未识别"));}
    @Test void abortAndCloseReleaseRecognitionResources() throws Exception{Recognizer recognizer=mock(Recognizer.class);attributes.put("recognizer",recognizer);attributes.put("transcriptSegments",new ArrayList<>());text("{\"type\":\"abort\"}");verify(recognizer).close();assertFalse(attributes.containsKey("transcriptSegments"));assertEquals(Map.of("type","tts","state","stop"),sent.get(0));attributes.put("recognizer",recognizer);handler.afterConnectionClosed(session,CloseStatus.NORMAL);verify(recognizer,times(2)).close();}
}
