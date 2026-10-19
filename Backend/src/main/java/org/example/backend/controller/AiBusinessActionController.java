package org.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.result.Result;
import org.example.backend.security.AuthenticationAttributes;
import org.example.backend.service.AiBusinessOperationService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class AiBusinessActionController {
    private final AiBusinessOperationService operations;
    public AiBusinessActionController(AiBusinessOperationService operations) { this.operations = operations; }

    @PostMapping("/ai-assistant/actions/{id}/decision")
    public Result decide(@PathVariable String id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Object raw = request.getAttribute(AuthenticationAttributes.USER_ID);
        if (!(raw instanceof Number number)) throw new IllegalArgumentException("请先登录");
        String conversationId = String.valueOf(body.getOrDefault("conversationId", ""));
        if (!(body.get("confirmed") instanceof Boolean)) throw new IllegalArgumentException("请选择确认或取消");
        return Result.success("操作已处理", operations.decide(number.intValue(), conversationId, id,
                Boolean.TRUE.equals(body.get("confirmed")), request));
    }
}
