package org.example.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.security.SecurityAccessPolicy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;

/** Only registered business endpoints can be called. Writes require a short-lived, single-use user confirmation. */
@Service
public class AiBusinessOperationService {
    private static final Map<String, String> MODULES = Map.ofEntries(
            Map.entry("labs", "实验室资源"), Map.entry("reservations", "实验室预约"),
            Map.entry("devices", "设备资产"), Map.entry("device-status", "设备状态"),
            Map.entry("device-inventory-records", "设备盘点"), Map.entry("device-transfer-records", "设备调拨"),
            Map.entry("consumables", "耗材库存与借还"), Map.entry("course-environment-requests", "课程环境"),
            Map.entry("environment-templates", "环境模板"), Map.entry("lab-software", "教学软件"),
            Map.entry("maintenance-records", "维护记录"), Map.entry("repairs", "故障报修"),
            Map.entry("class-timetables", "校园课表"), Map.entry("academic-schedules", "教务课表"),
            Map.entry("schedule-adjustments", "调课申请"), Map.entry("approval-countersigns", "业务会签"),
            Map.entry("usage-records", "使用记录"), Map.entry("notices", "通知公告"),
            Map.entry("iot", "物联网"), Map.entry("users", "用户管理"),
            Map.entry("activation-codes", "激活码管理"), Map.entry("system-settings", "系统设置"),
            Map.entry("business-loop", "业务协同"), Map.entry("business-reminders", "业务提醒"),
            Map.entry("dashboard", "统计概览"), Map.entry("modules", "通用业务记录"));
    private static final Pattern SECRET = Pattern.compile("(?i).*(password|secret|token|apiKey|credential|privateKey).*");
    private static final Map<String, String> MODULE_PERMISSIONS = Map.ofEntries(
            Map.entry("labs", "实验室管理"), Map.entry("reservations", "预约管理,教师课表"),
            Map.entry("devices", "设备资产"), Map.entry("device-status", "设备状态,接入授权,设备绑定"),
            Map.entry("device-inventory-records", "设备资产"), Map.entry("device-transfer-records", "设备资产"),
            Map.entry("consumables", "耗材库存"), Map.entry("course-environment-requests", "环境管理,课程环境"),
            Map.entry("environment-templates", "环境管理,课程环境"), Map.entry("lab-software", "环境管理"),
            Map.entry("maintenance-records", "维护记录管理,维护记录填写,维护记录查看,故障报修"),
            Map.entry("repairs", "维护记录管理,维护记录查看,故障报修"), Map.entry("class-timetables", "课表抓取,课表数据,教师课表"),
            Map.entry("academic-schedules", "课表抓取,课表数据,教师课表"), Map.entry("schedule-adjustments", "预约管理,教师课表"),
            Map.entry("approval-countersigns", "预约管理,环境管理"), Map.entry("usage-records", "使用记录"),
            Map.entry("notices", "通知公告"), Map.entry("iot", "物联网扩展"), Map.entry("users", "用户管理,用户权限"),
            Map.entry("activation-codes", "激活管理,接入授权,设备绑定,设备状态"), Map.entry("system-settings", "系统设置"),
            Map.entry("business-loop", "统计分析,平台统计"), Map.entry("business-reminders", "通知公告"),
            Map.entry("dashboard", "统计分析,平台统计"));
    private final ObjectProvider<RequestMappingHandlerMapping> mappings;
    private final AiBusinessDataService data;
    private final Clock clock;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final SecurityAccessPolicy policy = new SecurityAccessPolicy();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final ConcurrentHashMap<String, Pending> pending = new ConcurrentHashMap<>();
    private record Operation(String id, String module, String method, String path, HandlerMethod handler) {}
    private record Pending(Integer userId, String conversationId, Instant expiresAt, String operationId,
                           String path, Map<String, Object> body, Map<String, Object> query,
                           String snapshotOperationId, String snapshotPath, Object snapshot) {}

    @org.springframework.beans.factory.annotation.Autowired
    public AiBusinessOperationService(@Qualifier("requestMappingHandlerMapping") ObjectProvider<RequestMappingHandlerMapping> mappings,
                                      AiBusinessDataService data) {
        this(mappings, data, Clock.systemUTC());
    }

    AiBusinessOperationService(ObjectProvider<RequestMappingHandlerMapping> mappings, AiBusinessDataService data, Clock clock) {
        this.mappings = mappings; this.data = data; this.clock = clock;
    }

    private Map<String, Operation> operations(Integer userId) {
        AiDataActor actor = data.actor(userId);
        Map<String, Operation> result = new TreeMap<>();
        mappings.getObject().getHandlerMethods().forEach((mapping, handler) -> {
            if (!handler.getBeanType().getPackageName().equals("org.example.backend.controller")) return;
            for (String path : mapping.getPatternValues()) {
                String module = path.split("/", 3).length > 1 ? path.split("/", 3)[1] : "";
                if (!MODULES.containsKey(module) || SECRET.matcher(path).matches()) continue;
                if (!actor.permissions().isEmpty()) {
                    String labels = path.equals("/users/options") ? "用户管理,用户权限,实验室管理" : MODULE_PERMISSIONS.getOrDefault(module, "");
                    if (Arrays.stream(labels.split(",")).noneMatch(actor.permissions()::contains)) continue;
                }
                // Uploads require file selection; credentials are deliberately handled by their own forms.
                if (Arrays.stream(handler.getMethodParameters()).anyMatch(p ->
                        p.getParameterType().getName().contains("MultipartFile"))) continue;
                for (RequestMethod method : mapping.getMethodsCondition().getMethods()) {
                    String probe = path.replaceAll("\\{[^}]+}", "1");
                    if (!Set.of("GET", "POST", "PUT", "PATCH", "DELETE").contains(method.name())
                            || !policy.isAllowed(method.name(), probe, actor.role())) continue;
                    String id = method.name() + " " + path;
                    result.put(id, new Operation(id, module, method.name(), path, handler));
                }
            }
        });
        return result;
    }

    public List<Map<String, String>> modules(Integer userId) {
        return operations(userId).values().stream().map(Operation::module).distinct()
                .map(module -> Map.of("module", module, "name", MODULES.get(module))).toList();
    }

    public List<Map<String, Object>> catalog(Integer userId, String module) {
        return operations(userId).values().stream().filter(op -> op.module().equals(module)).map(op -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("operationId", op.id()); item.put("confirmationRequired", !op.method().equals("GET"));
            Map<String, Object> query = new LinkedHashMap<>();
            for (var parameter : op.handler().getMethodParameters()) {
                if (parameter.hasParameterAnnotation(RequestBody.class))
                    item.put("body", schema(parameter.getParameterType(), 0));
                RequestParam param = parameter.getParameterAnnotation(RequestParam.class);
                if (param != null) query.put(!param.value().isBlank() ? param.value() :
                        !param.name().isBlank() ? param.name() : Objects.toString(parameter.getParameterName(), "parameter"),
                        Map.of("type", parameter.getParameterType().getSimpleName(), "required", param.required(), "default", param.defaultValue()));
            }
            item.put("query", query);
            item.put("pathParameters", op.path());
            return item;
        }).toList();
    }

    private Object schema(Class<?> type, int depth) {
        if (Map.class.isAssignableFrom(type)) return "object（字段以查询到的现有记录及业务表单为准，不可猜测）";
        if (type.isPrimitive() || type.getPackageName().startsWith("java.") || type.isEnum() || depth >= 3)
            return type.getSimpleName();
        Map<String, Object> fields = new LinkedHashMap<>();
        json.getDeserializationConfig().introspect(json.constructType(type)).findProperties().forEach(property -> {
            if (!SECRET.matcher(property.getName()).matches() && property.getPrimaryType() != null)
                fields.put(property.getName(), schema(property.getPrimaryType().getRawClass(), depth + 1));
        });
        return fields;
    }

    public void invalidate(Integer userId, String conversationId) {
        Instant now = clock.instant();
        pending.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now)
                || (entry.getValue().userId().equals(userId) && entry.getValue().conversationId().equals(conversationId)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(Object value) {
        if (value == null) return Map.of();
        if (!(value instanceof Map<?, ?>)) throw new IllegalArgumentException("操作参数必须为对象");
        return json.convertValue(value, LinkedHashMap.class);
    }

    private String resolve(Operation op, Map<String, Object> values) {
        Matcher matcher = Pattern.compile("\\{([^}:]+)(?::[^}]+)?}").matcher(op.path());
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String value = Objects.toString(values.get(matcher.group(1)), "");
            if (!value.matches("[\\p{L}\\p{N}_-]+")) throw new IllegalArgumentException("请明确操作目标：" + matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        String path = result.toString();
        String route = op.path().replaceAll("\\{[^}]+}", "[^/]+");
        if (!path.matches(route)) throw new IllegalArgumentException("操作目标无效");
        return path;
    }

    private Operation require(Integer userId, String id) {
        Operation op = operations(userId).get(id);
        if (op == null) throw new IllegalArgumentException("当前账号无权执行此操作或接口尚未接入");
        return op;
    }

    public Map<String, Object> prepare(Integer userId, String conversationId, Map<String, Object> action, HttpServletRequest request) {
        Operation op = require(userId, Objects.toString(action.get("operationId"), ""));
        if (op.method().equals("GET")) throw new IllegalArgumentException("查询不需要确认");
        String path = resolve(op, object(action.get("pathValues")));
        Map<String, Object> body = object(action.get("body")), query = object(action.get("query"));
        if (containsSecret(body) || containsSecret(query)) throw new IllegalArgumentException("密码、密钥等敏感设置请在对应表单操作");
        if (json.valueToTree(body).toString().length() > 24000) throw new IllegalArgumentException("本次操作内容过多，请缩小范围");
        invalidate(userId, conversationId);
        if (pending.size() >= 1000) throw new IllegalArgumentException("待确认操作过多，请稍后重试");
        String token = UUID.randomUUID().toString();
        Instant expires = clock.instant().plusSeconds(600);
        Operation snapshotOperation = null;
        String snapshotPath = path;
        Object snapshot = null;
        if (!op.method().equals("POST") || !path.equals("/" + op.module())) {
            Map<String, Operation> allowed = operations(userId);
            for (String candidate : List.of(path, path.substring(0, path.lastIndexOf('/')))) {
                snapshotOperation = allowed.values().stream().filter(read -> read.method().equals("GET")
                        && candidate.matches(read.path().replaceAll("\\{[^}]+}", "[^/]+"))).findFirst().orElse(null);
                if (snapshotOperation != null) { snapshotPath = candidate; break; }
            }
            if (snapshotOperation != null) snapshot = invoke(snapshotOperation, snapshotPath, Map.of(), Map.of(), request);
        }
        pending.put(token, new Pending(userId, conversationId, expires, op.id(), path, body, query,
                snapshotOperation == null ? "" : snapshotOperation.id(), snapshotPath, snapshot));
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", token); view.put("module", MODULES.get(op.module())); view.put("method", op.method());
        view.put("target", path); view.put("body", body); view.put("query", query); view.put("expiresAt", expires.toString());
        view.put("summary", Objects.toString(action.get("summary"), "请核对操作内容"));
        if (snapshot != null) view.put("before", snapshot);
        view.put("warning", op.method().equals("DELETE") && op.module().equals("labs")
                ? "删除实验室将同时删除关联设备、预约、报修、课程环境和软件记录。" : "确认后将按以下内容执行，最终结果以业务校验为准。");
        return view;
    }

    private boolean containsSecret(Object value) {
        if (value instanceof Map<?, ?> map) return map.entrySet().stream().anyMatch(entry ->
                SECRET.matcher(String.valueOf(entry.getKey())).matches() || containsSecret(entry.getValue()));
        return value instanceof List<?> list && list.stream().anyMatch(this::containsSecret);
    }

    public Object read(Integer userId, Map<String, Object> query, HttpServletRequest request) {
        Operation op = require(userId, Objects.toString(query.get("operationId"), ""));
        if (!op.method().equals("GET")) throw new IllegalArgumentException("查询协议不能执行写入操作");
        return invoke(op, resolve(op, object(query.get("pathValues"))), Map.of(), object(query.get("query")), request);
    }

    public Object decide(Integer userId, String conversationId, String token, boolean confirmed, HttpServletRequest request) {
        Pending action = pending.get(token);
        if (action == null || !action.userId().equals(userId) || !action.conversationId().equals(conversationId))
            throw new IllegalArgumentException("操作不存在、已处理或不属于当前对话，请重新生成");
        if (!action.expiresAt().isAfter(clock.instant())) {
            pending.remove(token); throw new IllegalArgumentException("确认已过期，请重新生成操作");
        }
        Operation op = require(userId, action.operationId());
        if (!pending.remove(token, action)) throw new IllegalArgumentException("操作已处理，请勿重复提交");
        if (!confirmed) return Map.of("message", "已取消，未执行操作");
        if (action.snapshot() != null) {
            Object latest = invoke(require(userId, action.snapshotOperationId()), action.snapshotPath(), Map.of(), Map.of(), request);
            if (!Objects.equals(latest, action.snapshot())) throw new IllegalArgumentException("目标记录已变化，请重新查询并确认操作");
        }
        return invoke(op, action.path(), action.body(), action.query(), request);
    }

    private Object invoke(Operation op, String path, Map<String, Object> body, Map<String, Object> query, HttpServletRequest request) {
        try {
            String authorization = request.getHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) throw new IllegalArgumentException("请重新登录后操作");
            var uri = org.springframework.web.util.UriComponentsBuilder.fromUriString("http://127.0.0.1:" + request.getLocalPort())
                    .path(request.getContextPath()).path(path);
            query.forEach((key, value) -> { if (value instanceof List<?> list) list.forEach(v -> uri.queryParam(key, v)); else uri.queryParam(key, value); });
            HttpRequest.Builder call = HttpRequest.newBuilder(URI.create(uri.build().encode().toUriString()))
                    .timeout(Duration.ofSeconds(30)).header("Authorization", authorization).header("Content-Type", "application/json");
            call.method(op.method(), op.method().equals("GET") ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            HttpResponse<String> response = client.send(call.build(), HttpResponse.BodyHandlers.ofString());
            Map<String, Object> payload = object(json.readValue(response.body(), Map.class));
            if (response.statusCode() >= 300 || (payload.containsKey("code") && !"200".equals(String.valueOf(payload.get("code")))))
                throw new IllegalArgumentException(Objects.toString(payload.get("message"), "业务校验失败"));
            return redact(payload);
        } catch (IllegalArgumentException exception) { throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt(); throw new IllegalArgumentException("请求中断，请到业务列表核实结果后再操作");
        } catch (Exception exception) {
            throw new IllegalArgumentException("业务接口未返回明确结果，请到业务列表核实后再操作，避免重复提交");
        }
    }

    private Object redact(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> { if (!SECRET.matcher(String.valueOf(key)).matches()) result.put(String.valueOf(key), redact(item)); });
            return result;
        }
        if (value instanceof List<?> list) return list.stream().map(this::redact).toList();
        return value;
    }
}
