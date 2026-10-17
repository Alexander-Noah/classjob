package org.example.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.backend.entity.UserEntity;
import org.example.backend.mapper.AiScheduleReadMapper;
import org.example.backend.mapper.ClassTimetableMapper;
import org.example.backend.mapper.UserMapper;
import org.example.backend.security.SecurityAccessPolicy;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/** Explicit read-only data catalog: no SQL, URL, table or arbitrary bean name supplied by the model. */
@Service
public class AiBusinessDataService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final ApplicationContext beans;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
    private final SecurityAccessPolicy policy = new SecurityAccessPolicy();
    private final Map<String, Dataset> datasets = new LinkedHashMap<>();
    private final Clock clock;

    private record Dataset(String id, String title, String path, Set<String> permissions,
                           BiFunction<AiDataActor, Map<String, Object>, Object> read) {}

    /**
     * 构造 AiBusinessDataService，初始化当前实例。
     * @param beans beans参数。
     */
    @org.springframework.beans.factory.annotation.Autowired
    public AiBusinessDataService(ApplicationContext beans) { this(beans, Clock.system(ZONE)); }
    /**
     * 构造 AiBusinessDataService，初始化当前实例。
     * @param beans beans参数。
     * @param clock 时钟参数。
     */
    AiBusinessDataService(ApplicationContext beans, Clock clock) {
        this.beans = beans;
        this.clock = clock;
        register("labs", "实验室资源、容量、位置、开放状态、负责人", "/labs", "实验室管理",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(LabRecordReadService.class).findAll(actor.labScope().managerUserId()));
        register("timetable", "校园课表：课程、教师、班级、教室、周次、节次；mode=now查当前正在上课，date查某天", "/class-timetables", "课表抓取,课表数据,教师课表",
                this::readSchedule);
        register("teaching-calendar", "当前学期及开学日期", "/class-timetables", "课表抓取,课表数据,教师课表",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(TeachingScheduleReadService.class).findActiveCalendar());
        register("timetable-sync-config", "课表同步周期、启用状态及学期配置", "/class-timetables/crawler-config", "课表抓取,课表数据",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(ClassTimetableMapper.class).getCrawlerConfig("lab-admin-class-timetable"));
        register("timetable-sync-tasks", "最近200次课表同步任务的状态、更新数量和完成时间", "/class-timetables/crawler-tasks", "课表抓取,课表数据",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(ClassTimetableMapper.class).listCrawlerTasks(200).stream()
                        .map(/** 将当前条目转换为目标结果。 入参：row。 */ row -> project(row, Set.of("id", "triggerType", "status", "scope", "source", "attempts", "updatedCount", "startedAt", "finishedAt"))).toList());
        register("reservations", "实验室预约、审批、冲突、申请人、日期时段", "/reservations", "预约管理,教师课表",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(ReservationCatalogQuery.class).list(ReservationAccessScope.forActor(actor.reservationActor())));
        register("schedule-adjustments", "调课申请、审核、执行状态", "/schedule-adjustments", "预约管理,教师课表",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(ScheduleAdjustmentService.class).listAdjustments());
        register("consumables", "耗材库存、预警、品类、存放位置", "/consumables", "耗材库存",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(ConsumableService.class).listConsumables());
        register("consumable-stock-records", "耗材出入库流水", "/consumables/stock-records", "耗材库存",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(ConsumableService.class).listStockRecords(optionalLong(q, "consumableId")));
        register("consumable-loans", "耗材借用、归还、逾期", "/consumables/loans", "耗材库存",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(ConsumableLoanService.class).list());
        register("course-environments", "课程环境需求、配置进度、验收状态", "/course-environment-requests", "环境管理,课程环境",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(CourseEnvironmentService.class).getEnvironment());
        register("environment-templates", "实验环境模板、系统、软件清单", "/environment-templates", "环境管理,课程环境",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(EnvironmentTemplateService.class).listTemplates());
        register("software", "教学软件、版本、安装情况", "/lab-software", "环境管理",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(softwareService.class).getLabSoftware().getData());
        register("maintenance", "实验室维护记录、处理进度、维护人员", "/maintenance-records", "维护记录管理,维护记录填写,维护记录查看,故障报修",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(MaintenanceRecordService.class).listMaintenanceRecords());
        register("repairs", "故障报修、维修状态", "/repairs", "维护记录管理,维护记录查看,故障报修",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(RepairService.class).listRepairs());
        register("usage-records", "实验室使用记录、签到、使用时长", "/usage-records", "使用记录",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(UsageRecordService.class).listUsageRecords());
        register("notices", "通知公告内容、发布状态（管理员列表或当前用户收件箱）", "/notices/user", "通知公告",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> actor.systemAdmin() || "labAdmin".equals(actor.role())
                        ? bean(NoticeService.class).listNotices()
                        : bean(NoticeService.class).listUserNotices(actor.userId(), actor.role(), null));
        register("notice-receipts", "当前用户通知已读、未读记录", "/notices/user", "通知公告",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(NoticeService.class).listUserNotices(actor.userId(), actor.role(), null));
        register("approval-countersigns", "业务会签、待办、审批意见", "/approval-countersigns", "预约管理,环境管理",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(ApprovalCountersignService.class).listCountersigns(null, null, null, null));
        register("runtime-summary", "设备运行汇总：在线、离线、异常；不含资产信息", "/device-status/summary", "设备状态",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(DeviceStatusService.class).getSummary(stringQuery(q)));
        register("runtime-events", "设备运行告警和状态事件", "/device-status/events", "设备状态",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(DeviceStatusService.class).listEvents(stringQuery(q)));
        register("runtime-status", "指定实验室设备运行状态，需要labId", "/device-status/labs", "设备状态",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(DeviceStatusService.class).listLabRuntimeStatus(requiredLab(actor, q), stringQuery(q)));
        register("iot-devices", "物联网设备在线状态及传感器类型，不含资产与连接凭证", "/iot/devices", "物联网扩展",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(IotDeviceService.class).listDevices(stringQuery(q)));
        register("iot-commands", "物联网控制历史日志", "/iot/command-logs", "物联网扩展",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(IotDeviceService.class).listCommandLogs(stringQuery(q)));
        register("iot-environment", "实验室温湿度等环境感知数据，需要labId", "/iot/environment", "物联网扩展",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(IotDeviceService.class).getEnvironment(requiredLab(actor, q)));
        register("teacher-host", "实验室教师主机在线状态，需要labId", "/device-status/labs", "设备状态",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(TeacherHostService.class).currentHost(requiredLab(actor, q)));
        register("student-clients", "实验室学生终端在线状态，需要labId", "/device-status/labs", "设备状态",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(StudentClientService.class).listByLabId(requiredLab(actor, q)));
        register("users", "用户姓名、角色、部门和启用状态", "/users/options", "用户管理,用户权限,实验室管理",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> bean(UserService.class).listUsers().stream().map(this::userOption).toList());
        module("system-settings", "平台系统设置（隐去密钥）", "/system-settings", "系统设置");
        module("activation-codes", "接入授权状态、到期时间、绑定情况（隐去激活码）", "/admin/activation-codes", "激活管理");
        module("activation-code-audit-logs", "接入授权审计日志（隐去凭证）", "/admin/activation-codes", "激活管理");
        register("lab-authorizations", "当前账号实验室的接入授权状态和到期时间（不含激活码）", "/activation-codes/lab-bindings", "激活管理,接入授权,设备绑定,设备状态",
                /**
                 * 处理 register 调用中的回调逻辑。
                 * 入参：actor、q。
                 */
                (actor, q) -> {
                    Set<String> labs = bean(LabRecordReadService.class).findAll(actor.labScope().managerUserId()).stream()
                            .map(/** 将当前条目转换为目标结果。 入参：lab。 */ lab -> String.valueOf(lab.id())).collect(Collectors.toSet());
                    return bean(ModuleRecordService.class).listModuleRecords("activation-codes").stream()
                            .filter(/** 判断当前条目是否符合筛选条件。 入参：row。 */ row -> !Boolean.TRUE.equals(row.get("deleted")) && labs.contains(Objects.toString(row.get("labId"), "")))
                            .map(/** 将当前条目转换为目标结果。 入参：row。 */ row -> project(row, Set.of("id", "labId", "labCode", "labName", "labBoundAt", "deviceLimit", "enabled", "status", "statusKey", "statusText", "expireAt", "expiresAt"))).toList();
                });
        module("operation-events", "非资产业务操作日志", "/business-loop", "统计分析,平台统计");
    }

    /**
     * bean的处理逻辑。
     * @param type 类型参数。
     * @return T 类型的处理结果。
     */
    private <T> T bean(Class<T> type) { return beans.getBean(type); }
    /**
     * 注册当前操作。
     * @param id 标识参数。
     * @param title title参数。
     * @param path 路径参数。
     * @param permissions 权限参数。
     * @param reader reader参数。
     */
    private void register(String id, String title, String path, String permissions,
                          BiFunction<AiDataActor, Map<String, Object>, Object> reader) {
        datasets.put(id, new Dataset(id, title, path, Set.of(permissions.split(",")), reader));
    }
    /**
     * module的处理逻辑。
     * @param id 标识参数。
     * @param title title参数。
     * @param path 路径参数。
     * @param permissions 权限参数。
     */
    private void module(String id, String title, String path, String permissions) {
        register(id, title, path, permissions, /** 处理 register 调用中的回调逻辑。 入参：actor、q。 */ (actor, q) -> bean(ModuleRecordService.class).listModuleRecords(id));
    }

    /**
     * 操作者的处理逻辑。
     * @param userId 用户标识参数。
     * @return AiDataActor 类型的处理结果。
     */
    public AiDataActor actor(Integer userId) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("请先登录");
        UserEntity user = bean(UserMapper.class).getUser(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())
                || !Set.of("systemAdmin", "labAdmin", "teacher", "academic").contains(Objects.toString(user.getRoleCode(), ""))) {
            throw new IllegalArgumentException("账号不可用，请重新登录");
        }
        Set<String> permissions = new HashSet<>();
        if (user.getPermissions() != null && !user.getPermissions().isBlank()) {
            try {
                Object parsed = json.readValue(user.getPermissions(), Object.class);
                if (!(parsed instanceof List<?>)) throw new IllegalArgumentException("权限配置必须是列表");
                collectPermissions(parsed, permissions);
            }
            catch (Exception e) { throw new IllegalArgumentException("账号权限配置无效，请联系管理员"); }
        }
        return new AiDataActor(userId, user.getRoleCode(),
                user.getRealName() == null ? user.getUsername() : user.getRealName(), Set.copyOf(permissions));
    }
    /**
     * 收集权限。
     * @param value 值参数。
     * @param result 结果参数。
     */
    private void collectPermissions(Object value, Set<String> result) {
        if (value instanceof String text) result.add(text);
        else if (value instanceof List<?> list) list.forEach(/** 处理遍历到的当前条目。 入参：item。 */ item -> collectPermissions(item, result));
        else if (value instanceof Map<?, ?> map && map.get("items") != null) collectPermissions(map.get("items"), result);
    }

    /**
     * allowed的处理逻辑。
     * @param dataset dataset参数。
     * @param actor 操作者参数。
     * @return 当前判断的布尔结果。
     */
    private boolean allowed(Dataset dataset, AiDataActor actor) {
        return actor.userId() != null && actor.userId() > 0
                && policy.isAllowed("GET", dataset.path(), actor.role())
                && (actor.permissions().isEmpty() || !Collections.disjoint(dataset.permissions(), actor.permissions()));
    }
    /**
     * catalog的处理逻辑。
     * @param actor 操作者参数。
     * @return 当前操作得到的记录集合。
     */
    public List<Map<String, String>> catalog(AiDataActor actor) {
        return datasets.values().stream().filter(/** 判断当前条目是否符合筛选条件。 入参：dataset。 */ dataset -> allowed(dataset, actor))
                .map(/** 将当前条目转换为目标结果。 入参：dataset。 */ dataset -> Map.of("dataset", dataset.id(), "description", dataset.title())).toList();
    }

    /**
     * 查询当前操作。
     * @param actor 操作者参数。
     * @param request 请求参数。
     * @return 包含 status、excluded、message、forbidden、limit、offset、dataset、source、queriedAt、timezone、department、role、category、type、labId、lab、teacher、c 等字段的结果对象。
     */
    public Map<String, Object> query(AiDataActor actor, Map<String, Object> request) {
        String id = text(request, "dataset");
        Dataset dataset = datasets.get(id);
        if (dataset == null) return Map.of("status", "excluded", "message", "该数据不在可查询范围内，资产数据禁止接入");
        if (!allowed(dataset, actor)) return Map.of("status", "forbidden", "message", "当前账号没有此模块的查询权限");
        Map<String, Object> q = new LinkedHashMap<>(request);
        int limit = integer(q.get("limit"), 50, 1, 100);
        int offset = integer(q.get("offset"), 0, 0, 1000000);
        q.put("limit", limit);
        q.put("offset", offset);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataset", id);
        result.put("source", dataset.title());
        result.put("queriedAt", now().toString());
        result.put("timezone", ZONE.toString());
        try {
            Object raw = dataset.read().apply(actor, q);
            Object safe = sanitize(json.convertValue(raw, Object.class), id);
            if (safe instanceof Map<?, ?> map && map.containsKey("records") && map.containsKey("hasMore")) {
                map.forEach(/** 处理遍历到的当前条目。 入参：key、value。 */ (key, value) -> result.put(String.valueOf(key), value));
            } else {
                List<?> rows = safe instanceof List<?> list ? list : rowsFromMap(safe);
                if (rows != null) {
                    List<?> matching = rows.stream().filter(/** 判断当前条目是否符合筛选条件。 入参：row。 */ row -> matches(row, q)).toList();
                    String groupBy = text(q, "groupBy");
                    if (groupBy != null) {
                        if (!Set.of("status", "department", "role", "category", "type", "labId", "lab", "teacher", "classroom", "courseName", "className", "onlineStatus", "runtimeStatus", "maintainer", "person").contains(groupBy))
                            throw new AiQueryException("不支持按此字段统计");
                        Map<String, Long> groups = matching.stream().collect(Collectors.groupingBy(
                                /**
                                 * 处理 groupingBy 调用中的回调逻辑。
                                 * 入参：row。
                                 */
                                row -> row instanceof Map<?, ?> map ? Objects.toString(map.get(groupBy), "未填写") : "未填写",
                                LinkedHashMap::new, Collectors.counting()));
                        result.put("groupBy", groupBy);
                        result.put("groups", groups);
                    }
                    int from = Math.min(offset, matching.size());
                    int to = Math.min(from + limit, matching.size());
                    result.put("records", matching.subList(from, to));
                    result.put("total", matching.size());
                    result.put("offset", from);
                    result.put("hasMore", to < matching.size());
                    result.put("nextOffset", to);
                    if (Set.of("runtime-events", "iot-commands", "timetable-sync-tasks").contains(id)) {
                        result.put("scope", "仅最近的事件记录；不能据此计算全历史统计");
                        result.put("totalIsExact", false);
                    }
                    if (safe instanceof Map<?, ?> map && map.get("total") instanceof Number total
                            && total.longValue() > rows.size()) result.put("sourceTotal", total);
                } else result.put("data", safe == null ? Map.of() : safe);
            }
            result.putIfAbsent("status", "ok");
        } catch (Exception e) {
            // Do not forward SQL errors, connection details, or credentials into model context.
            result.put("status", "unavailable");
            result.put("message", e instanceof AiQueryException ? e.getMessage() : "该模块数据暂时查询失败，请稍后重试");
        }
        return result;
    }
    /**
     * 行集合来源映射的处理逻辑。
     * @param value 值参数。
     * @return 当前操作得到的记录集合。
     */
    private List<?> rowsFromMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            for (String key : List.of("records", "devices", "clients", "list")) {
                if (map.get(key) instanceof List<?> rows) return rows;
            }
        }
        return null;
    }
    /**
     * 判断是否匹配当前操作。
     * @param row 行参数。
     * @param q q参数。
     * @return 当前判断的布尔结果。
     */
    private boolean matches(Object row, Map<String, Object> q) {
        String keyword = text(q, "keyword");
        if (keyword != null && !encode(row).toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) return false;
        if (row instanceof Map<?, ?> map) {
            for (String key : List.of("id", "labId", "status", "consumableId")) {
                String expected = text(q, key);
                if (expected != null && !expected.equals(Objects.toString(map.get(key), ""))) return false;
            }
            String date = text(q, "date");
            if (date != null && List.of("date", "reservationDate", "createdAt", "occurredAt", "startDate", "useDate").stream()
                    .noneMatch(/** 处理 noneMatch 调用中的回调逻辑。 入参：key。 */ key -> Objects.toString(map.get(key), "").startsWith(date))) return false;
        }
        return true;
    }
    /**
     * 字符串查询的处理逻辑。
     * @param q q参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, String> stringQuery(Map<String, Object> q) {
        Map<String, String> result = new HashMap<>();
        for (String key : List.of("labId", "deviceId", "sourceType", "eventType", "eventLevel", "status", "limit")) {
            if (text(q, key) != null) result.put(key, text(q, key));
        }
        return result;
    }
    /**
     * required实验室的处理逻辑。
     * @param actor 操作者参数。
     * @param q q参数。
     * @return Long 类型的处理结果。
     */
    private Long requiredLab(AiDataActor actor, Map<String, Object> q) {
        Long id = optionalLong(q, "labId");
        if (id == null) throw new AiQueryException("请先查询实验室列表，提供labId后再查询此数据");
        if (!actor.systemAdmin() && bean(LabRecordReadService.class).findById(id, (long) actor.userId()) == null)
            throw new AiQueryException("无权查询该实验室数据");
        return id;
    }
    /**
     * 用户选项的处理逻辑。
     * @param user 用户参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, Object> userOption(UserEntity user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId()); result.put("name", user.getRealName());
        result.put("role", user.getRoleName()); result.put("department", user.getDepartment()); result.put("status", user.getStatus());
        return result;
    }
    /**
     * project的处理逻辑。
     * @param row 行参数。
     * @param keys 键参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, Object> project(Map<String, Object> row, Set<String> keys) {
        Map<String, Object> result = new LinkedHashMap<>();
        keys.stream().sorted().filter(row::containsKey).forEach(/** 处理遍历到的当前条目。 入参：key。 */ key -> result.put(key, row.get(key)));
        return result;
    }

    /**
     * 读取课表。
     * @param actor 操作者参数。
     * @param q q参数。
     * @return Object 类型的处理结果。
     */
    private Object readSchedule(AiDataActor actor, Map<String, Object> q) {
        List<LabRecord> boundLabs = bean(LabRecordReadService.class).findAll(actor.labScope().managerUserId());
        Long requestedLab = optionalLong(q, "labId");
        if (requestedLab != null) boundLabs = boundLabs.stream().filter(/** 判断当前条目是否符合筛选条件。 入参：lab。 */ lab -> requestedLab.equals(lab.id())).toList();
        if (boundLabs.isEmpty()) return Map.of("records", List.of(), "total", 0, "hasMore", false,
                "scope", "当前账号绑定的实验室", "message", "当前账号尚未绑定实验室，请先在实验室资源中绑定");
        List<String> patterns = boundLabs.stream().flatMap(/** 转换并展开当前条目。 入参：lab。 */ lab -> labAliases(lab).stream())
                .distinct().map(this::labPattern).toList();
        if (patterns.isEmpty()) throw new AiQueryException("绑定的实验室尚未填写教室编号，暂时无法匹配课表");
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("labPatterns", patterns);
        for (String key : List.of("semester", "teacher", "classroom", "courseName", "className", "keyword", "week", "limit", "offset")) {
            Object value = q.get(key);
            filters.put(key, value == null || String.valueOf(value).isBlank() ? null : value);
        }
        ZonedDateTime now = now();
        boolean current = "now".equals(text(q, "mode"));
        String dateText = current ? now.toLocalDate().toString() : text(q, "date");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("scope", "仅当前账号绑定的实验室，不含未绑定教室");
        if (dateText != null) {
            LocalDate date = LocalDate.parse(dateText);
            ScheduleCalendarData calendar = bean(TeachingScheduleReadService.class).findActiveCalendar();
            if (calendar == null || calendar.semesterStartDate() == null || calendar.semester() == null
                    || calendar.semesterStartDate().isBlank() || calendar.semester().isBlank())
                throw new AiQueryException("未配置当前学期或开学日期，无法准确计算教学周，请先在课表采集页面配置");
            long days = ChronoUnit.DAYS.between(LocalDate.parse(calendar.semesterStartDate()), date);
            if (days < 0) throw new AiQueryException("查询日期早于当前学期开学日期");
            filters.put("semester", calendar.semester());
            filters.put("week", days / 7 + 1);
            filters.put("weekday", "星期" + "一二三四五六日".charAt(date.getDayOfWeek().getValue() - 1));
            metadata.put("date", dateText); metadata.put("academicWeek", days / 7 + 1);
            metadata.put("semester", calendar.semester());
            if (current) {
                Integer section = TeachingSectionTimes.currentSection(now.toLocalTime());
                if (section == null) return Map.of("records", List.of(), "total", 0, "hasMore", false,
                        "message", "当前时间不在平台配置的授课时段内", "date", dateText);
                filters.put("sectionStart", section); filters.put("sectionEnd", section + 1);
                metadata.put("timeRange", TeachingSectionTimes.start(section) + "-" + TeachingSectionTimes.end(section + 1));
            }
        }
        AiScheduleReadMapper mapper = bean(AiScheduleReadMapper.class);
        List<LabRecord> scopeLabs = boundLabs;
        long total = mapper.count(filters);
        List<ScheduleEntryView> records = mapper.search(filters);
        String groupBy = text(q, "groupBy");
        if (groupBy != null) {
            if (!Set.of("teacher", "classroom", "courseName", "className", "weekday").contains(groupBy))
                throw new AiQueryException("课表支持按teacher,classroom,courseName,className,weekday统计");
            filters.put("groupBy", groupBy);
            metadata.put("groupBy", groupBy);
            List<Map<String, Object>> groups = mapper.aggregate(filters);
            if ("classroom".equals(groupBy)) {
                Map<String, Long> merged = new LinkedHashMap<>();
                for (Map<String, Object> group : groups) {
                    for (LabRecord lab : matchingLabs(Objects.toString(group.get("label"), ""), scopeLabs)) {
                        String label = Objects.toString(lab.labName(), lab.labCode());
                        merged.merge(label, ((Number) group.get("count")).longValue(), Long::sum);
                    }
                }
                metadata.put("groups", merged);
            } else metadata.put("groups", groups);
        }
        metadata.put("records", records.stream().map(/** 将当前条目转换为目标结果。 入参：row。 */ row -> readableSchedule(row, scopeLabs)).toList());
        metadata.put("total", total);
        metadata.put("offset", filters.get("offset"));
        long next = ((Number) filters.get("offset")).longValue() + records.size();
        metadata.put("hasMore", next < total); metadata.put("nextOffset", next);
        metadata.put("meaning", "按平台课表计算的计划授课情况，不代表实际签到或实时到课");
        return metadata;
    }

    /**
     * 实验室Aliases的处理逻辑。
     * @param lab 实验室参数。
     * @return 当前操作得到的记录集合。
     */
    private Set<String> labAliases(LabRecord lab) {
        Set<String> aliases = new LinkedHashSet<>();
        for (String value : List.of(Objects.toString(lab.labCode(), ""), Objects.toString(lab.labName(), ""))) {
            if (!value.isBlank()) aliases.add(value.trim());
            var code = java.util.regex.Pattern.compile("(?<![0-9])(\\d+-\\d+)[mM]?(?![0-9])").matcher(value);
            if (code.find()) aliases.add(code.group(1));
        }
        String room = Objects.toString(lab.roomNo(), "").trim();
        if (room.contains("-") || aliases.isEmpty()) {
            if (!room.isBlank()) aliases.add(room);
        } else if (!room.isBlank()) {
            var building = java.util.regex.Pattern.compile("\\d+").matcher(Objects.toString(lab.building(), ""));
            if (building.find()) aliases.add(building.group() + "-" + room);
        }
        return aliases;
    }

    /**
     * 实验室Pattern的处理逻辑。
     * @param alias alias参数。
     * @return String 类型的处理结果。
     */
    private String labPattern(String alias) {
        // Bind regex values rather than interpolating SQL; accept the campus M suffix, not similar room numbers.
        String escaped = alias.replaceAll("([\\\\.\\[\\]{}()*+?^$|])", "\\\\$1");
        return "(^|[^A-Za-z0-9])" + escaped + "[mM]?($|[^A-Za-z0-9])";
    }

    /**
     * readable课表的处理逻辑。
     * @param row 行参数。
     * @param labs 实验室参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, Object> readableSchedule(ScheduleEntryView row, List<LabRecord> labs) {
        Map<String, Object> result = json.convertValue(row, new TypeReference<Map<String, Object>>() {});
        List<LabRecord> matched = matchingLabs(Objects.toString(row.classroom(), ""), labs);
        String classroom = matched.stream().map(/** 将当前条目转换为目标结果。 入参：lab。 */ lab -> Objects.toString(lab.labCode(), Objects.toString(lab.roomNo(), "")))
                .distinct().collect(Collectors.joining("、"));
        result.put("classroom", classroom);
        result.put("labName", matched.stream().map(/** 将当前条目转换为目标结果。 入参：lab。 */ lab -> Objects.toString(lab.labName(), lab.labCode()))
                .distinct().collect(Collectors.joining("、")));
        if (row.startSection() != null && row.endSection() != null && row.startSection() >= 1 && row.endSection() <= 12) {
            result.put("timeRange", TeachingSectionTimes.start(row.startSection()) + "—" + TeachingSectionTimes.end(row.endSection()));
        }
        return result;
    }

    /**
     * matching实验室的处理逻辑。
     * @param classroom classroom参数。
     * @param labs 实验室参数。
     * @return 当前操作得到的记录集合。
     */
    private List<LabRecord> matchingLabs(String classroom, List<LabRecord> labs) {
        return labs.stream().filter(/** 判断当前条目是否符合筛选条件。 入参：lab。 */ lab -> labAliases(lab).stream()
                .anyMatch(/** 处理 anyMatch 调用中的回调逻辑。 入参：alias。 */ alias -> java.util.regex.Pattern.compile(labPattern(alias), java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(classroom).find())).toList();
    }

    /** Strip excluded fields recursively; drop business records originating in asset workflows. */
    Object sanitize(Object value, String dataset) {
        if (value instanceof Map<?, ?> map) {
            if (assetOrigin(map)) return null;
            Map<String, Object> safe = new LinkedHashMap<>();
            map.forEach(/** 处理遍历到的当前条目。 入参：key、item。 */ (key, item) -> {
                String name = String.valueOf(key);
                if (!blockedField(name, dataset)) {
                    Object clean = sanitize(item, dataset);
                    if (clean != null) safe.put(name, clean);
                }
            });
            return safe;
        }
        if (value instanceof List<?> list) return list.stream().map(/** 将当前条目转换为目标结果。 入参：item。 */ item -> sanitize(item, dataset)).filter(Objects::nonNull).toList();
        if (value instanceof String text && (text.trim().startsWith("{") || text.trim().startsWith("["))) {
            try { return sanitize(json.readValue(text, Object.class), dataset); }
            catch (Exception ignored) { /* Keep ordinary text as text. */ }
        }
        if (value instanceof String text && text.length() > 2000) return text.substring(0, 2000) + "…（字段内容已截断）";
        return value;
    }
    /**
     * 资产Origin的处理逻辑。
     * @param row 行参数。
     * @return 当前判断的布尔结果。
     */
    private boolean assetOrigin(Map<?, ?> row) {
        for (String key : List.of("sourceModule", "businessType", "module", "moduleName", "source", "target", "category", "subject")) {
            String text = Objects.toString(row.get(key), "").toLowerCase(Locale.ROOT);
            if (text.matches(".*(asset|inventory-record|transfer-record|设备资产|资产台账|资产盘点|资产调拨).*")) return true;
            if (Set.of("devices", "device", "device-inventory", "device-transfer", "activation-code").contains(text)) return true;
        }
        for (String key : List.of("title", "content", "subject")) {
            if (Objects.toString(row.get(key), "").matches("(?s).*(资产|设备采购|设备盘点|设备调拨).*")) return true;
        }
        return false;
    }
    /**
     * blockedField的处理逻辑。
     * @param field field参数。
     * @param dataset dataset参数。
     * @return 当前判断的布尔结果。
     */
    private boolean blockedField(String field, String dataset) {
        String key = field.replaceAll("[_-]", "").toLowerCase(Locale.ROOT);
        if (key.matches(".*(password|secret|token|credential|apikey|accesskey|authorization|activationcode|licensekey|privatekey|cipher|asset|purchase|depreciation|serialnumber|inventoryrecord|transferrecord|资产|密码|密钥|口令|激活码).*")) return true;
        if (dataset.equals("system-settings") && Set.of("key", "auth").contains(key)) return true;
        if (Set.of("devices", "devicecount", "deviceprice", "snapshoturl", "streamurl", "baseurl", "endpoint", "connectionconfig", "authvalue", "authconfig", "headers", "requestparamsjson", "responseresultjson", "payloadjson", "errormessage").contains(key))
            return !(key.equals("devices") && Set.of("runtime-status", "iot-devices").contains(dataset));
        return dataset.startsWith("activation-") && Set.of("code", "key", "mac", "machinemac", "fingerprint").contains(key);
    }
    /**
     * now的处理逻辑。
     * @return ZonedDateTime 类型的处理结果。
     */
    public ZonedDateTime now() { return ZonedDateTime.now(clock).withZoneSameInstant(ZONE); }
    /**
     * 编码当前操作。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    public String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalArgumentException("数据序列化失败"); }
    }
    /**
     * 文本的处理逻辑。
     * @param q q参数。
     * @param key 键参数。
     * @return String 类型的处理结果。
     */
    static String text(Map<String, Object> q, String key) {
        String value = Objects.toString(q.get(key), "").trim(); return value.isEmpty() ? null : value;
    }
    /**
     * optional长整数的处理逻辑。
     * @param q q参数。
     * @param key 键参数。
     * @return Long 类型的处理结果。
     */
    private Long optionalLong(Map<String, Object> q, String key) {
        String value = text(q, key); if (value == null) return null;
        long id = Long.parseLong(value); if (id <= 0) throw new AiQueryException("数据ID无效"); return id;
    }
    /**
     * integer的处理逻辑。
     * @param value 值参数。
     * @param fallback fallback参数。
     * @param min min参数。
     * @param max max参数。
     * @return int 类型的处理结果。
     */
    private int integer(Object value, int fallback, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(String.valueOf(value)))); }
        catch (Exception e) { return fallback; }
    }
    private static class AiQueryException extends IllegalArgumentException {
        /**
         * 构造 AiQueryException，初始化当前实例。
         * @param message 消息参数。
         */
        AiQueryException(String message) { super(message); }
    }
}
