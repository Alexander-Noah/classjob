/*
 * AI 助手实现模块。
 * 主要协作组件：TongyiAgentService、BusinessLoopService。
 */
package org.example.backend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.backend.service.AiAssistantService;
import org.example.backend.service.BusinessLoopService;
import org.example.backend.service.TongyiAgentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private final TongyiAgentService tongyiAgentService;
    private final BusinessLoopService businessLoopService;

    /**
     * 构造 AiAssistantServiceImpl，初始化当前实例。
     * @param tongyiAgentService tongyiAgent服务参数。
     * @param businessLoopService businessLoop服务参数。
     */
    public AiAssistantServiceImpl(
            TongyiAgentService tongyiAgentService,
            BusinessLoopService businessLoopService
    ) {
        this.tongyiAgentService = tongyiAgentService;
        this.businessLoopService = businessLoopService;
    }

    /**
     * generateAnswer的处理逻辑。
     * @param question question参数。
     * @param scene scene参数。
     * @return String 类型的处理结果。
     */
    @Override
    public String generateAnswer(String question, String scene) {
        return generateAnswer(question, scene, List.of());
    }

    /**
     * generateAnswer的处理逻辑。
     * @param question question参数。
     * @param scene scene参数。
     * @param images images参数。
     * @return String 类型的处理结果。
     */
    @Override
    public String generateAnswer(String question, String scene, List<Map<String, Object>> images) {
        if (!tongyiAgentService.isConfigured()) {
            recordAiEvent(scene, question, "未配置");
            return "通义智能体尚未在后端配置，请在服务器环境变量或 application.yaml 中配置 TONGYI_API_KEY 和 TONGYI_AGENT_APP_ID。";
        }

        try {
            String answer = tongyiAgentService.callAgent(
                    question,
                    scene,
                    images == null ? List.of() : images
            );

            recordAiEvent(scene, question, "已回复");
            return answer;
        } catch (Exception exception) {
            log.warn("Tongyi agent call failed", exception);
            recordAiEvent(scene, question, "调用失败");

            if (images != null && !images.isEmpty()) {
                return "图片识别调用失败，请确认通义视觉模型已启用，或在 application.yaml / 环境变量中配置 TONGYI_VISION_MODEL。";
            }

            return "通义智能体调用失败，请检查学校服务器网络、公司 API Key、智能体 APP_ID 和阿里云百炼服务状态。";
        }
    }

    /**
     * 记录AI事件。
     * @param scene scene参数。
     * @param question question参数。
     * @param status 状态参数。
     */
    private void recordAiEvent(String scene, String question, String status) {
        businessLoopService.recordEvent(
                "ai-assistant",
                "consult",
                firstNonBlank(scene, "智能问答"),
                status,
                Map.of(
                        "scene", firstNonBlank(scene, "default"),
                        "questionLength", question == null ? 0 : question.length()
                )
        );
    }

    /**
     * first非空白的处理逻辑。
     * @return String 类型的处理结果。
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}