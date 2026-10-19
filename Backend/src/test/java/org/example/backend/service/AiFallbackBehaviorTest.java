package org.example.backend.service;
import org.example.backend.service.impl.AiAssistantServiceImpl;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AiFallbackBehaviorTest {
    final TongyiAgentService provider=mock(TongyiAgentService.class);
    final BusinessLoopService loop=mock(BusinessLoopService.class);
    final AiAssistantService service=new AiAssistantServiceImpl(provider,loop);
    @Test void unavailableProviderExplainsConfigurationAndAuditsWithoutSending(){assertTrue(service.generateAnswer(null,null).contains("尚未在后端配置"));verify(provider,never()).callAgent(any(),any(),any());verify(loop).recordEvent("ai-assistant","consult","智能问答","未配置",Map.of("scene","default","questionLength",0));}
    @Test void providerSuccessReturnsItsAnswerAndNormalizesNullImages(){when(provider.isConfigured()).thenReturn(true);when(provider.callAgent("问题","教学",List.of())).thenReturn("答复");assertEquals("答复",service.generateAnswer("问题","教学",null));verify(loop).recordEvent("ai-assistant","consult","教学","已回复",Map.of("scene","教学","questionLength",2));}
    @Test void providerFailuresProduceDifferentTextAndVisionGuidance(){when(provider.isConfigured()).thenReturn(true);when(provider.callAgent(any(),any(),any())).thenThrow(new IllegalStateException("offline"));assertTrue(service.generateAnswer("问题","教学").contains("调用失败"));assertTrue(service.generateAnswer("图片","教学",List.of(Map.of("dataUrl","image"))).contains("视觉模型"));verify(loop,times(2)).recordEvent(eq("ai-assistant"),eq("consult"),eq("教学"),eq("调用失败"),anyMap());}
}
