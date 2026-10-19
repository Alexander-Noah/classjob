package org.example.backend.service;
import org.example.backend.entity.AiAssistantUserConfigEntity;
import org.example.backend.mapper.AiAssistantUserConfigMapper;
import org.example.backend.service.impl.AiAssistantUserConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.*;
import org.springframework.http.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiUserConfigTransportBehaviorTest {
    @Test void absentConfigsReturnDisabledDefaultsAndInvalidIdentityNeverReachesProvider(){Map<String,Object> defaults=service.getConfig(2);assertEquals(false,defaults.get("enabled"));assertEquals(false,defaults.get("apiKeyConfigured"));assertEquals("qwen-plus",defaults.get("model"));assertThrows(IllegalArgumentException.class,()->service.chat(2,"问题"));assertThrows(IllegalArgumentException.class,()->service.getConfig(0));assertThrows(IllegalArgumentException.class,()->service.getConfig(null));service.saveConfig(2,Map.of("enabled","false"));verify(mapper).upsertConfig(argThat(config->Boolean.FALSE.equals(config.getEnabled())));verifyNoInteractions(client);}
    final AiAssistantUserConfigMapper mapper=mock(AiAssistantUserConfigMapper.class);
    final RestClient client=mock(RestClient.class);
    final RestClient.RequestBodyUriSpec request=mock(RestClient.RequestBodyUriSpec.class,RETURNS_SELF);
    final RestClient.ResponseSpec response=mock(RestClient.ResponseSpec.class);
    AiAssistantUserConfigServiceImpl service;
    @BeforeEach void setup(){service=new AiAssistantUserConfigServiceImpl(mapper);ReflectionTestUtils.setField(service,"restClient",client);AiAssistantUserConfigEntity config=new AiAssistantUserConfigEntity();config.setUserId(1);config.setEnabled(true);config.setBaseUrl("https://api.test/v1/");config.setModel("model");config.setApiKey("test-key");when(mapper.getByUserId(1)).thenReturn(config);when(client.post()).thenReturn(request);when(request.retrieve()).thenReturn(response);}
    private void answer(Object content){when(response.body(Map.class)).thenReturn(Map.of("choices",List.of(Map.of("message",Map.of("content",content)))));}
    @Test void requestsNormalizeEndpointAndUseConfiguredKeyAndContext(){answer(" OK ");assertEquals("OK",service.chat(1,"问题"));verify(request).uri("https://api.test/v1/chat/completions");verify(request).header(HttpHeaders.AUTHORIZATION,"Bearer test-key");assertEquals("OK",service.chatWithContext(1,"业务数据","问题"));var body=ArgumentCaptor.forClass(Object.class);verify(request,times(2)).body(body.capture());assertTrue(body.getValue().toString().contains("业务数据"));assertTrue(body.getValue().toString().contains("问题"));}
    @Test void connectionTestsTruncatePreviewAndSupportDraftConfiguration(){answer("x".repeat(90));Map<String,Object> result=service.testConnection(1);assertEquals(true,result.get("connected"));assertEquals(80,result.get("responsePreview").toString().length());assertEquals("https://api.test/v1",result.get("baseUrl"));answer("OK");assertEquals("new",service.testConnection(1,Map.of("model","new")).get("model"));verify(mapper,never()).upsertConfig(any());}
    @Test void structuredContentConcatenatesTextAndIgnoresUnsupportedEntries(){answer(List.of(Map.of("text","甲"),"ignored",Map.of("content","乙"),Map.of("text"," ")));assertEquals("甲\n乙",service.chat(1,null));for(Map<?,?> payload:List.of(Map.of(),Map.of("choices",List.of()),Map.of("choices",List.of(Map.of("message",Map.of()))))){when(response.body(Map.class)).thenReturn(payload);assertThrows(IllegalArgumentException.class,()->service.chat(1,"问题"));}when(response.body(Map.class)).thenReturn(null);assertThrows(IllegalArgumentException.class,()->service.chat(1,"问题"));}
    @Test void remoteErrorsUseProviderMessageAndTruncateVerboseResponses(){for(String body:List.of("{\"message\":\"wrong key\"}","{\"msg\":\"model missing\"}","x".repeat(300),"")){doThrow(new RestClientResponseException("bad",400,"Bad Request",HttpHeaders.EMPTY,body.getBytes(StandardCharsets.UTF_8),StandardCharsets.UTF_8)).when(response).body(Map.class);String message=assertThrows(IllegalArgumentException.class,()->service.chat(1,"问题")).getMessage();assertFalse(message.isBlank());assertTrue(message.length()<=240);if(body.contains("wrong key"))assertEquals("wrong key",message);if(body.contains("model missing"))assertEquals("model missing",message);}doThrow(new RestClientException("offline")).when(response).body(Map.class);assertTrue(assertThrows(IllegalArgumentException.class,()->service.chat(1,"问题")).getMessage().contains("offline"));}
}

