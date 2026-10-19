package org.example.backend.service;
import org.example.backend.service.impl.TongyiAgentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.*;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TongyiAgentBehaviorTest {
    @Test void persistedProviderSettingsLoadWithoutExposingTheirKey() throws Exception {
        Path target = Path.of("data", "tongyi-api-config.json");
        try (var files = mockStatic(Files.class)) {
            files.when(() -> Files.exists(target)).thenReturn(true);
            files.when(() -> Files.readString(target)).thenReturn("{\"enabled\":true,\"model\":\"saved-model\",\"vision_model\":\"saved-vision\",\"base_url\":\"https://saved.test/v1\",\"api_key\":\"saved-only-key\"}");
            var config = new TongyiAgentServiceImpl(new ObjectMapper(), new MockEnvironment()).getConfig();
            assertEquals("saved-model", config.get("model"));
            assertEquals("saved-vision", config.get("vision_model"));
            assertEquals("https://saved.test/v1", config.get("base_url"));
            assertEquals(true, config.get("configured"));
            assertFalse(config.toString().contains("saved-only-key"));
        }
    }
    @Test void malformedProviderFileFallsBackToUsableDefaults() throws Exception {
        Path target = Path.of("data", "tongyi-api-config.json");
        try (var files = mockStatic(Files.class)) {
            files.when(() -> Files.exists(target)).thenReturn(true);
            files.when(() -> Files.readString(target)).thenReturn("not-json");
            var config = new TongyiAgentServiceImpl(new ObjectMapper(), new MockEnvironment()).getConfig();
            assertEquals("qwen-plus", config.get("model"));
            assertEquals("qwen-vl-plus", config.get("vision_model"));
            assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1", config.get("base_url"));
        }
    }
    final MockEnvironment env=new MockEnvironment().withProperty("tongyi.agent.api-key","test-only-key").withProperty("tongyi.agent.base-url","https://api.test/v1///").withProperty("tongyi.agent.model","text-model").withProperty("tongyi.agent.vision-model","vision-model");
    final RestClient client=mock(RestClient.class);
    final RestClient.RequestBodyUriSpec request=mock(RestClient.RequestBodyUriSpec.class,RETURNS_SELF);
    final RestClient.ResponseSpec response=mock(RestClient.ResponseSpec.class);
    TongyiAgentServiceImpl service;
    @BeforeEach void setup(){service=new TongyiAgentServiceImpl(new ObjectMapper(),env);ReflectionTestUtils.setField(service,"restClient",client);ReflectionTestUtils.setField(service,"fileConfig",new HashMap<>(Map.of("enabled",true,"api_key","file-key")));when(client.post()).thenReturn(request);when(request.retrieve()).thenReturn(response);when(response.body(String.class)).thenReturn("{\"choices\":[{\"message\":{\"content\":\"实验室开放\"}}]}");}
    @Test void textRequestsUseConfiguredModelBearerHeaderAndNoSecretInConfigResponse(){assertEquals("实验室开放",service.callAgent("状态","session"));verify(request).uri("https://api.test/v1/chat/completions");verify(request).header(HttpHeaders.AUTHORIZATION,"Bearer test-only-key");verify(request).contentType(MediaType.APPLICATION_JSON);var body=ArgumentCaptor.forClass(Object.class);verify(request).body(body.capture());Map<?,?> payload=(Map<?,?>)body.getValue();assertEquals("text-model",payload.get("model"));assertEquals(0.7,payload.get("temperature"));assertFalse(service.getConfig().toString().contains("test-only-key"));assertEquals(true,service.getConfig().get("api_key_configured"));}
    @Test void validImagesSwitchToVisionModelAndAreLimitedToThree(){List<Map<String,Object>> images=new ArrayList<>(Arrays.asList(null,Map.of("dataUrl","https://remote.test/image.png")));for(int index=0;index<4;index++)images.add(Map.of("dataUrl","data:image/png;base64,"+index));service.callAgent("",null,images);var body=ArgumentCaptor.forClass(Object.class);verify(request).body(body.capture());Map<?,?> payload=(Map<?,?>)body.getValue();assertEquals("vision-model",payload.get("model"));List<?> messages=(List<?>)payload.get("messages");List<?> content=(List<?>)((Map<?,?>)messages.get(1)).get("content");assertEquals(4,content.size());assertEquals("请分析这张图片。",((Map<?,?>)content.get(0)).get("text"));}
    @Test void disabledProviderCannotSendAndNullImageListsRemainTextOnly(){env.withProperty("tongyi.agent.enabled","false");assertFalse(service.isConfigured());assertThrows(IllegalStateException.class,()->service.callAgent("问题",null));verifyNoInteractions(client);env.withProperty("tongyi.agent.enabled","1");assertTrue(service.isConfigured());assertEquals("实验室开放",service.callAgent(null,null,null));}
    @Test void blankMalformedAndMissingResponseContentFailExplicitly(){for(String payload:Arrays.asList(null," ","bad json","{}","{\"choices\":[{\"message\":{\"content\":\"\"}}]}")){when(response.body(String.class)).thenReturn(payload);assertThrows(IllegalStateException.class,()->service.callAgent("问题",null));}}
    @Test void configurationUpdatesPreserveMaskedKeysAndNeverWriteToRealFilesystem(){Path target=Path.of("data","tongyi-api-config.json");try(var files=mockStatic(Files.class)){Map<String,Object> result=service.updateConfig(Map.of("api_key","******","model","new-file-model","enabled",false));files.verify(()->Files.createDirectories(target.getParent()));files.verify(()->Files.writeString(eq(target),argThat(text->text.toString().contains("file-key")&&text.toString().contains("new-file-model"))));assertEquals(false,result.get("configured"));assertFalse(result.containsKey("api_key"));}}
    @Test void configurationWriteErrorsAreReportedWithoutLeakingCredentials(){Path target=Path.of("data","tongyi-api-config.json");try(var files=mockStatic(Files.class)){files.when(()->Files.createDirectories(target.getParent())).thenThrow(new java.io.IOException("denied"));assertThrows(IllegalStateException.class,()->service.updateConfig(Map.of("model","new-model")));}}
}
