package org.example.backend.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.backend.entity.*;
import java.lang.reflect.*;
import java.util.*;

/** Bounded JSON input plus domain checks; SQL parameterization and output escaping remain required. */
public final class RequestInputValidation {
    public static final int MAX_BODY_BYTES = 12 * 1024 * 1024;
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();
    static {
        JSON.enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        JSON.enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        JSON.getFactory().setStreamReadConstraints(com.fasterxml.jackson.core.StreamReadConstraints.builder()
                .maxNestingDepth(25).maxStringLength(8 * 1024 * 1024).maxNumberLength(100).build());
    }
    private RequestInputValidation() {}

    public static void websocketText(String payload) {
        if (payload == null || payload.length() > MAX_BODY_BYTES) fail("消息过长");
        String trimmed = payload.stripLeading();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            json(JSON.valueToTree(Map.of("text", payload)));
            return;
        }
        try {
            JsonNode root = JSON.readTree(payload);
            json(root);
            if (!root.isObject()) fail("消息必须为 JSON 对象");
            for (String field : List.of("type", "state", "text"))
                if (root.has(field) && !root.get(field).isTextual()) fail("消息类型、状态和文本必须为字符串");
            if (!root.has("type") || !Set.of("hello", "abort", "listen").contains(root.path("type").asText())) fail("不支持的消息类型");
            if (root.path("type").asText().equals("listen") && !Set.of("start", "stop", "detect").contains(root.path("state").asText())) fail("不支持的语音状态");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) { fail("消息 JSON 格式错误或字段重复"); }
    }

    public static void json(JsonNode root) {
        if (!root.isObject() && !root.isArray()) fail("请求正文必须是对象或数组");
        visit(root, "请求", "", 0, new int[]{0});
    }

    private static void visit(JsonNode node, String path, String field, int depth, int[] count) {
        if (depth > 20 || ++count[0] > 50000) fail("请求结构过于复杂，请分批提交");
        if (!node.isNull() && Set.of("quantity", "deviceLimit", "port", "capacity", "stock", "warnThreshold", "progress", "flowStep", "returnedQuantity", "remainingQuantity", "usageHours").contains(field)
                && (!node.isIntegralNumber() || !node.canConvertToInt()))
            fail(path + "必须填写范围内的整数，不能使用小数或字符串");
        if (node.isObject()) {
            if (node.size() > 200) fail(path + "字段过多");
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                if (key.length() > 100 || key.codePoints().anyMatch(Character::isISOControl)
                        || Set.of("__proto__", "prototype", "constructor").contains(key)) fail("请求包含无效字段名");
                visit(entry.getValue(), path + "." + key, key, depth + 1, count);
            });
        } else if (node.isArray()) {
            if (node.size() > 1000) fail(path + "单次最多提交 1000 项");
            for (JsonNode child : node) visit(child, path + "[]", field, depth + 1, count);
        } else if (node.isTextual()) {
            String value = node.textValue();
            int max = path.endsWith(".image_url.url") || path.endsWith(".images[].url") ? 8 * 1024 * 1024 : stringLimit(field);
            if (value.length() > max) fail(label(field) + "不能超过 " + max + " 个字符");
            if (value.codePoints().anyMatch(c -> (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t')))
                fail(label(field) + "不能包含非法控制字符");
        } else if (node.isNumber() && !node.isBigDecimal() && !Double.isFinite(node.doubleValue())) {
            fail(path + "必须是有限数值");
        }
    }

    public static int stringLimit(String field) {
        return switch (field.toLowerCase(Locale.ROOT)) {
            case "password", "oldpassword", "newpassword", "confirmpassword" -> 128;
            case "username", "realname", "teachername", "applicant", "borrower", "reporter" -> 100;
            case "phone" -> 40;
            case "email" -> 255;
            case "department" -> 160;
            case "avatarurl" -> 500;
            case "name", "devicename", "labname", "coursename", "course", "classname" -> 160;
            case "code", "devicecode", "labcode", "category", "unit", "type", "labtype" -> 100;
            case "title" -> 255;
            case "status", "processstatus", "confirmstatus", "publishstatus", "tagtype" -> 40;
            case "remark", "description", "reason", "purpose", "reviewremark" -> 4000;
            case "captchaid" -> 36;
            case "captchacode" -> 4;
            case "html" -> 2 * 1024 * 1024;
            case "images", "image", "imageurl", "image_url", "dataurl", "audio", "data" -> 8 * 1024 * 1024;
            case "content", "text", "answer", "question", "systemprompt", "system_prompt" -> 100000;
            default -> 16000;
        };
    }
    private static String label(String field) {
        return switch (field.toLowerCase(Locale.ROOT)) {
            case "username" -> "账号";
            case "password", "oldpassword", "newpassword", "confirmpassword" -> "密码";
            case "name", "labname", "devicename" -> "名称";
            case "phone" -> "联系电话";
            case "email" -> "邮箱";
            case "department" -> "部门";
            case "title" -> "标题";
            case "remark", "description", "reason" -> "说明内容";
            default -> "填写内容";
        };
    }

    /** Reject fractional IDs and scalar coercion before the MVC converter can truncate them. */
    public static void types(JsonNode node, Type type) {
        if (node == null || node.isNull()) return;
        if (type instanceof ParameterizedType pt) {
            if (pt.getRawType() instanceof Class<?> raw && Map.class.isAssignableFrom(raw)) {
                if (!node.isObject()) fail("请求字段类型不正确");
                node.elements().forEachRemaining(v -> types(v, pt.getActualTypeArguments()[1]));
                return;
            }
            if (pt.getRawType() instanceof Class<?> raw && Collection.class.isAssignableFrom(raw)) {
                if (!node.isArray()) fail("请求字段必须为数组");
                node.forEach(v -> types(v, pt.getActualTypeArguments()[0]));
                return;
            }
        }
        if (!(type instanceof Class<?> cls) || cls == Object.class) return;
        if (cls.isEnum()) { if (!node.isTextual()) fail("枚举字段必须为文本"); return; }
        if (cls == String.class) { if (!node.isTextual()) fail("文本字段不能填写数字、对象或数组"); return; }
        if (cls == Integer.class || cls == int.class || cls == Long.class || cls == long.class) {
            if (!node.isIntegralNumber() || (cls == Integer.class || cls == int.class ? !node.canConvertToInt() : !node.canConvertToLong()))
                fail("整数字段必须填写范围内的整数，不能使用小数或字符串");
            return;
        }
        if (cls == Boolean.class || cls == boolean.class) { if (!node.isBoolean()) fail("开关字段必须为 true 或 false"); return; }
        if (!cls.getPackageName().startsWith("org.example.backend")) return;
        if (!node.isObject()) fail("请求字段必须为对象");
        for (Field f : cls.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            JsonNode value = node.get(f.getName());
            var alias = f.getAnnotation(com.fasterxml.jackson.annotation.JsonAlias.class);
            if (alias != null) for (String name : alias.value()) {
                if (node.has(name) && !name.equals(f.getName())) {
                    if (value != null) fail("同一字段不能同时提交多个别名");
                    value = node.get(name);
                }
            }
            types(value, f.getGenericType());
        }
    }

    public static void domain(Object body) {
        domain(body, Collections.newSetFromMap(new IdentityHashMap<>()));
    }
    private static void domain(Object body, Set<Object> seen) {
        if (body == null || !seen.add(body)) return;
        if (body instanceof Map<?,?> map) { map.values().forEach(v -> domain(v, seen)); return; }
        if (body instanceof Collection<?> list) { list.forEach(v -> domain(v, seen)); return; }
        if (!body.getClass().getPackageName().startsWith("org.example.backend")) return;
        if (body instanceof UserEntity user) AccountInputValidation.profile(user);
        if (body instanceof TeacherHostEntity host) {
            positive("实验室编号", host.getLabId());
            AccountInputValidation.text("教师设备标识", host.getTeacherDeviceId(), 100, true);
            AccountInputValidation.text("主机地址", host.getHostIp(), 253, true);
            if (host.getPort() == null || host.getPort() < 1 || host.getPort() > 65535) fail("端口必须为 1 至 65535 的整数");
        }
        if (body instanceof StudentClientEntity client) {
            positive("实验室编号", client.getLabId());
            AccountInputValidation.text("学生设备标识", client.getStudentDeviceId(), 100, true);
        }
        if (body instanceof LabEntity lab) {
            AccountInputValidation.text("实验室名称", lab.getLabName(), 100, true);
            range("容量", lab.getCapacity(), 0, 100000);
            range("设备数", lab.getDeviceCount(), 0, 100000);
            choice("实验室状态", lab.getOpenStatus(), Set.of("开放", "预约中", "已预约", "使用中", "维护中", "停用", "暂停开放"));
        }
        if (body instanceof DevicesEntity device) {
            AccountInputValidation.text("设备名称", device.getDeviceName(), 160, true);
            range("设备数量", device.getQuantity(), 1, 1000000);
            range("使用时长", device.getUsageHours(), 0, Integer.MAX_VALUE);
        }
        if (body instanceof ConsumableEntity c) {
            AccountInputValidation.text("耗材名称", c.getName(), 100, true);
            range("库存", c.getStock(), 0, Integer.MAX_VALUE);
            range("预警阈值", c.getWarnThreshold(), 0, Integer.MAX_VALUE);
        }
        if (body instanceof ConsumableLoanEntity loan) {
            range("借出数量", loan.getQuantity(), 1, Integer.MAX_VALUE);
            range("归还数量", loan.getReturnedQuantity(), 0, Integer.MAX_VALUE);
            if (loan.getQuantity() != null && loan.getReturnedQuantity() != null && loan.getReturnedQuantity() > loan.getQuantity()) fail("归还数量不能超过借出数量");
        }
        if (body instanceof RepairEntity repair) range("维修进度", repair.getProgress(), 0, 100);
        if (body instanceof NoticeEntity notice) {
            AccountInputValidation.text("通知标题", notice.getTitle(), 255, true);
            AccountInputValidation.text("通知内容", notice.getContent(), 100000, true);
        }
        if (body instanceof CourseEnvironmentEntity course)
            AccountInputValidation.text("课程名称", course.getCourseName() == null || course.getCourseName().isBlank() ? course.getCourse() : course.getCourseName(), 160, true);
        if (body instanceof ScheduleAdjustmentEntity adjustment) {
            AccountInputValidation.text("课程名称", adjustment.getCourse(), 160, true);
            range("流程步骤", adjustment.getFlowStep(), 0, 100);
            choice("调课状态", adjustment.getStatus(), Set.of("审核中", "已通过", "已调整", "已取消", "已驳回"));
        }
        for (Field f : body.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive() || f.getType().isEnum()) continue;
            try { f.setAccessible(true); domain(f.get(body), seen); }
            catch (IllegalAccessException e) { throw new IllegalStateException("请求校验失败", e); }
        }
    }
    private static void positive(String name, Number value) { if (value == null || value.longValue() <= 0) fail(name + "必须为正整数"); }
    private static void range(String name, Integer value, int min, int max) { if (value != null && (value < min || value > max)) fail(name + "必须在 " + min + " 至 " + max + " 之间"); }
    private static void choice(String name, String value, Set<String> choices) { if (value != null && !value.isBlank() && !choices.contains(value)) fail(name + "不在允许范围内"); }
    private static void fail(String message) { throw new IllegalArgumentException(message); }
}
