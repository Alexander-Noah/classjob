/*
 * 工作台统计实现模块。
 * 主要协作组件：DashboardMapper、BusinessLoopReportService、LabService。
 */
package org.example.backend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.backend.entity.LabEntity;
import org.example.backend.mapper.DashboardMapper;
import org.example.backend.service.BusinessLoopReportService;
import org.example.backend.service.DashboardService;
import org.example.backend.service.LabService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {
    private final DashboardMapper dashboardMapper;
    private final BusinessLoopReportService businessLoopReportService;
    private final LabService labService;

    /**
     * 构造 DashboardServiceImpl，初始化当前实例。
     * @param dashboardMapper dashboard数据映射器参数。
     * @param businessLoopReportService businessLoopReport服务参数。
     * @param labService 实验室服务参数。
     */
    public DashboardServiceImpl(
            DashboardMapper dashboardMapper,
            BusinessLoopReportService businessLoopReportService,
            LabService labService
    ) {
        this.dashboardMapper = dashboardMapper;
        this.businessLoopReportService = businessLoopReportService;
        this.labService = labService;
    }

    /**
     * 获取概况。
     * @param managerUserId manager用户标识参数。
     * @return 当前操作返回的键值结果。
     */
    @Override
    public Map<String, Object> getOverview(Integer managerUserId) {
        Map<String, Object> overview = new LinkedHashMap<>();
        List<String> loadWarnings = new ArrayList<>();
        List<LabEntity> scopedLabs = safeLabList("实验室资源", /** 处理 safeLabList 调用中的回调逻辑。 */ () -> labService.getLabs(managerUserId), loadWarnings);
        Set<Long> activeLabIds = safeLongList("当前使用实验室", /** 处理 safeLongList 调用中的回调逻辑。 */ () -> dashboardMapper.listActiveLabIds(managerUserId), loadWarnings)
                .stream()
                .collect(Collectors.toSet());
        List<Map<String, Object>> rawTodaySchedules = safeList("今日安排", /** 处理 safeList 调用中的回调逻辑。 */ () -> dashboardMapper.listTodaySchedules(managerUserId), loadWarnings);
        List<Map<String, Object>> labTodaySchedules = buildTodaySchedules(scopedLabs, rawTodaySchedules, activeLabIds);

        overview.put("stats", buildStats(managerUserId, scopedLabs, activeLabIds, labTodaySchedules, loadWarnings));
        overview.put("shortcuts", buildShortcuts());
        putList(overview, "labUsage", "实验室使用概览", /** 处理 putList 调用中的回调逻辑。 */ () -> buildLabUsage(scopedLabs, activeLabIds), loadWarnings);
        putList(overview, "deviceStatus", "设备状态", /** 处理 putList 调用中的回调逻辑。 */ () -> dashboardMapper.listDeviceStatus(managerUserId), loadWarnings);
        putList(overview, "notices", "通知公告", dashboardMapper::listNotices, loadWarnings);
        putList(overview, "doorAlerts", "开门提醒", /** 处理 putList 调用中的回调逻辑。 */ () -> dashboardMapper.listDoorAlerts(managerUserId), loadWarnings);
        overview.put("todaySchedules", labTodaySchedules);
        putList(overview, "todoItems", "待处理事项", /** 处理 putList 调用中的回调逻辑。 */ () -> dashboardMapper.listTodoItems(managerUserId), loadWarnings);
        putList(overview, "recordReminders", "使用记录提醒", /** 处理 putList 调用中的回调逻辑。 */ () -> dashboardMapper.listRecordReminders(managerUserId), loadWarnings);
        putList(overview, "recentActivities", "最近动态", /** 处理 putList 调用中的回调逻辑。 */ () -> dashboardMapper.listRecentActivities(managerUserId), loadWarnings);
        overview.put("businessLoop", safeMap("业务闭环", businessLoopReportService::getOverview, loadWarnings));
        overview.put("loadWarnings", loadWarnings);
        return overview;
    }

    /**
     * 构建Stats。
     * @param managerUserId manager用户标识参数。
     * @param scopedLabs scoped实验室参数。
     * @param activeLabIds 活动实验室标识集合参数。
     * @param labTodaySchedules 实验室Today课表参数。
     * @param loadWarnings loadWarnings参数。
     * @return 当前操作得到的记录集合。
     */
    private List<Map<String, Object>> buildStats(
            Integer managerUserId,
            List<LabEntity> scopedLabs,
            Set<Long> activeLabIds,
            List<Map<String, Object>> labTodaySchedules,
            List<String> loadWarnings
    ) {
        long labCount = scopedLabs.size();
        long openLabs = scopedLabs.stream().filter(/** 判断当前条目是否符合筛选条件。 入参：lab。 */ lab -> isOpenLab(lab, activeLabIds)).count();
        long usingLabs = scopedLabs.stream().filter(/** 判断当前条目是否符合筛选条件。 入参：lab。 */ lab -> isUsingLab(lab, activeLabIds)).count();
        long deviceCount = safeLong("设备总数", /** 处理 safeLong 调用中的回调逻辑。 */ () -> dashboardMapper.countDevices(managerUserId), loadWarnings);
        long onlineDevices = safeLong("在线设备总数", /** 处理 safeLong 调用中的回调逻辑。 */ () -> dashboardMapper.countOnlineDevices(managerUserId), loadWarnings);
        long abnormalDevices = safeLong("异常设备总数", /** 处理 safeLong 调用中的回调逻辑。 */ () -> dashboardMapper.countAbnormalDevices(managerUserId), loadWarnings);
        long pendingReservations = safeLong("待审批预约总数", /** 处理 safeLong 调用中的回调逻辑。 */ () -> dashboardMapper.countPendingReservations(managerUserId), loadWarnings);
        long todayReservations = safeLong("今日预约总数", /** 处理 safeLong 调用中的回调逻辑。 */ () -> dashboardMapper.countTodayReservations(managerUserId), loadWarnings);
        long approvedTodayReservations = safeLong("今日已通过预约总数", /** 处理 safeLong 调用中的回调逻辑。 */ () -> dashboardMapper.countApprovedTodayReservations(managerUserId), loadWarnings);
        long activeRepairs = safeLong("处理中报修总数", /** 处理 safeLong 调用中的回调逻辑。 */ () -> dashboardMapper.countActiveRepairs(managerUserId), loadWarnings);
        long highPriorityRepairs = safeLong("高优先级报修总数", /** 处理 safeLong 调用中的回调逻辑。 */ () -> dashboardMapper.countHighPriorityRepairs(managerUserId), loadWarnings);
        long scheduledLabs = countScheduledLabs(labTodaySchedules);

        List<Map<String, Object>> stats = new ArrayList<>();
        stats.add(metric("实验室", labCount, "间", "lab", "#2563eb", "开放 " + openLabs + " 间"));
        stats.add(metric("当前使用实验室", usingLabs, "间", "reservation", "#0f766e", "今日预约 " + todayReservations + " 条"));
        stats.add(metric("今日有课实验室", scheduledLabs, "间", "record", "#7c3aed", "当前使用 " + usingLabs + " 间"));
        stats.add(metric("实验室预约", approvedTodayReservations, "条", "calendar", "#ea580c", "待审批 " + pendingReservations + " 条"));
        stats.add(metric("实验室待处理", pendingReservations + activeRepairs, "项", "todo", "#dc2626", "高优先级 " + highPriorityRepairs + " 项"));
        stats.add(metric("实验室设备", deviceCount, "台", "device", "#b45309", "异常 " + abnormalDevices + " 台，在线 " + onlineDevices + " 台"));
        return stats;
    }

    /**
     * 构建Today课表。
     * @param scopedLabs scoped实验室参数。
     * @param rawTodaySchedules 原始输入Today课表参数。
     * @param activeLabIds 活动实验室标识集合参数。
     * @return 当前操作得到的记录集合。
     */
    private List<Map<String, Object>> buildTodaySchedules(
            List<LabEntity> scopedLabs,
            List<Map<String, Object>> rawTodaySchedules,
            Set<Long> activeLabIds
    ) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LabEntity lab : scopedLabs) {
            List<Map<String, Object>> matchedSchedules = rawTodaySchedules.stream()
                    .filter(/** 判断当前条目是否符合筛选条件。 入参：schedule。 */ schedule -> isScheduleForLab(schedule, lab))
                    .sorted(Comparator.comparing(/** 处理 comparing 调用中的回调逻辑。 入参：schedule。 */ schedule -> String.valueOf(schedule.getOrDefault("time", ""))))
                    .map(/** 将当前条目转换为目标结果。 入参：schedule。 */ schedule -> buildTodayScheduleRow(lab, schedule))
                    .toList();
            if (matchedSchedules.isEmpty()) {
                rows.add(buildEmptyTodayScheduleRow(lab, activeLabIds));
            } else {
                rows.addAll(matchedSchedules);
            }
        }
        return rows;
    }

    /**
     * 构建Today课表行。
     * @param lab 实验室参数。
     * @param schedule 课表参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, Object> buildTodayScheduleRow(LabEntity lab, Map<String, Object> schedule) {
        Map<String, Object> row = new LinkedHashMap<>(schedule);
        row.put("labId", lab.getId());
        row.put("lab", resolveLabDisplay(lab));
        return row;
    }

    /**
     * 构建空值Today课表行。
     * @param lab 实验室参数。
     * @param activeLabIds 活动实验室标识集合参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, Object> buildEmptyTodayScheduleRow(LabEntity lab, Set<Long> activeLabIds) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("labId", lab.getId());
        row.put("time", "-");
        row.put("lab", resolveLabDisplay(lab));
        row.put("course", "今日暂无课程");
        row.put("className", "-");
        row.put("teacher", "-");
        row.put("status", isActiveLab(lab, activeLabIds) ? "使用中" : normalizeLabStatus(lab));
        row.put("statusType", isActiveLab(lab, activeLabIds) ? "success" : resolveStatusType(normalizeLabStatus(lab)));
        return row;
    }

    /**
     * 判断是否为课表For实验室。
     * @param schedule 课表参数。
     * @param lab 实验室参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isScheduleForLab(Map<String, Object> schedule, LabEntity lab) {
        Long scheduleLabId = toLong(schedule.get("labId"));
        if (scheduleLabId != null && lab.getId() != null && scheduleLabId.equals(Long.valueOf(lab.getId()))) {
            return true;
        }
        String scheduleLab = normalizeText(schedule.get("lab"));
        String scheduleClassroom = normalizeText(schedule.get("classroom"));
        return isSameText(scheduleLab, lab.getLabCode())
                || isSameText(scheduleLab, lab.getLabName())
                || isSameText(scheduleLab, lab.getRoomNo())
                || containsText(scheduleClassroom, lab.getLabName())
                || containsText(scheduleClassroom, lab.getLabCode())
                || containsText(scheduleClassroom, lab.getRoomNo());
    }

    /**
     * 判断是否具有课表课程。
     * @param row 行参数。
     * @return 当前判断的布尔结果。
     */
    private boolean hasScheduleCourse(Map<String, Object> row) {
        return !"今日暂无课程".equals(String.valueOf(row.get("course")));
    }

    /**
     * 统计Scheduled实验室。
     * @param rows 行集合参数。
     * @return long 类型的处理结果。
     */
    private long countScheduledLabs(List<Map<String, Object>> rows) {
        return rows.stream()
                .filter(this::hasScheduleCourse)
                .map(/** 将当前条目转换为目标结果。 入参：row。 */ row -> firstNonBlank(row.get("labId"), row.get("lab")))
                .filter(/** 判断当前条目是否符合筛选条件。 入参：value。 */ value -> !value.isBlank())
                .distinct()
                .count();
    }

    /**
     * 解析实验室Display。
     * @param lab 实验室参数。
     * @return String 类型的处理结果。
     */
    private String resolveLabDisplay(LabEntity lab) {
        return firstNonBlank(lab.getLabCode(), lab.getLabName(), lab.getRoomNo(), "LAB-" + lab.getId());
    }

    /**
     * 解析状态类型。
     * @param status 状态参数。
     * @return String 类型的处理结果。
     */
    private String resolveStatusType(String status) {
        if ("使用中".equals(status) || "占用中".equals(status) || "开放".equals(status)) return "success";
        if ("预约中".equals(status) || "待预约".equals(status) || "维护中".equals(status)) return "warning";
        if ("停用".equals(status)) return "info";
        return "primary";
    }

    /**
     * 判断是否为Same文本。
     * @param left left参数。
     * @param right right参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isSameText(String left, Object right) {
        String rightText = normalizeText(right);
        return !left.isBlank() && !rightText.isBlank() && left.equals(rightText);
    }

    /**
     * 判断是否包含文本。
     * @param left left参数。
     * @param right right参数。
     * @return 当前判断的布尔结果。
     */
    private boolean containsText(String left, Object right) {
        String rightText = normalizeText(right);
        return !left.isBlank() && !rightText.isBlank() && left.contains(rightText);
    }

    /**
     * 归一化文本。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String normalizeText(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toLowerCase().replaceAll("\\s+", "");
    }

    /**
     * 转换为长整数。
     * @param value 值参数。
     * @return Long 类型的处理结果。
     */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 构建实验室Usage。
     * @param scopedLabs scoped实验室参数。
     * @param activeLabIds 活动实验室标识集合参数。
     * @return 当前操作得到的记录集合。
     */
    private List<Map<String, Object>> buildLabUsage(List<LabEntity> scopedLabs, Set<Long> activeLabIds) {
        return scopedLabs.stream()
                .map(/** 将当前条目转换为目标结果。 入参：lab。 */ lab -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", firstNonBlank(lab.getLabCode(), lab.getLabName(), "LAB-" + lab.getId()));
                    item.put("value", resolveLabUsageValue(lab, activeLabIds));
                    return item;
                })
                .sorted(/** 处理 sorted 调用中的回调逻辑。 入参：left、right。 */ (left, right) -> Long.compare(
                        Number.class.cast(right.get("value")).longValue(),
                        Number.class.cast(left.get("value")).longValue()
                ))
                .limit(8)
                .toList();
    }

    /**
     * 判断是否为Open实验室。
     * @param lab 实验室参数。
     * @param activeLabIds 活动实验室标识集合参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isOpenLab(LabEntity lab, Set<Long> activeLabIds) {
        String status = normalizeLabStatus(lab);
        return !isLockedLab(status) && !isActiveLab(lab, activeLabIds);
    }

    /**
     * 判断是否为Using实验室。
     * @param lab 实验室参数。
     * @param activeLabIds 活动实验室标识集合参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isUsingLab(LabEntity lab, Set<Long> activeLabIds) {
        String status = normalizeLabStatus(lab);
        return !isLockedLab(status) && isActiveLab(lab, activeLabIds);
    }

    /**
     * 解析实验室Usage值。
     * @param lab 实验室参数。
     * @param activeLabIds 活动实验室标识集合参数。
     * @return int 类型的处理结果。
     */
    private int resolveLabUsageValue(LabEntity lab, Set<Long> activeLabIds) {
        String status = normalizeLabStatus(lab);
        if (!isLockedLab(status) && isActiveLab(lab, activeLabIds)) {
            return 88;
        }
        if (isLockedLab(status)) {
            return 12;
        }
        return 35;
    }

    /**
     * 判断是否为Locked实验室。
     * @param status 状态参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isLockedLab(String status) {
        return List.of("维护中", "停用").contains(status);
    }

    /**
     * 判断是否为活动实验室。
     * @param lab 实验室参数。
     * @param activeLabIds 活动实验室标识集合参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isActiveLab(LabEntity lab, Set<Long> activeLabIds) {
        if (lab == null || lab.getId() == null) {
            return false;
        }
        return activeLabIds.contains(Long.valueOf(lab.getId()));
    }

    /**
     * 归一化实验室状态。
     * @param lab 实验室参数。
     * @return String 类型的处理结果。
     */
    private String normalizeLabStatus(LabEntity lab) {
        return firstNonBlank(lab == null ? null : lab.getOpenStatus(), "开放");
    }

    /**
     * first非空白的处理逻辑。
     * @return String 类型的处理结果。
     */
    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            String text = value == null ? "" : String.valueOf(value).trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    /**
     * metric的处理逻辑。
     * @param title title参数。
     * @param value 值参数。
     * @param unit unit参数。
     * @param iconKey icon键参数。
     * @param color color参数。
     * @param trend trend参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, Object> metric(String title, long value, String unit, String iconKey, String color, String trend) {
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("title", title);
        metric.put("value", value);
        metric.put("unit", unit);
        metric.put("iconKey", iconKey);
        metric.put("color", color);
        metric.put("trend", trend);
        return metric;
    }

    /**
     * 构建Shortcuts。
     * @return 当前操作得到的记录集合。
     */
    private List<Map<String, Object>> buildShortcuts() {
        List<Map<String, Object>> shortcuts = new ArrayList<>();
        shortcuts.add(shortcut("设备资产", "资产台账与生命周期管理", "/lab-admin/devices", "device"));
        shortcuts.add(shortcut("设备状态", "在线、故障与维护监测", "/lab-admin/device-status", "normal"));
        shortcuts.add(shortcut("预约审批", "课程与自主预约审核", "/lab-admin/reservations", "reservation"));
        shortcuts.add(shortcut("维护记录查看", "实验室日常维护历史与检查结果", "/lab-admin/maintenance-records", "repair"));
        shortcuts.add(shortcut("使用记录", "上机、借用、巡检记录", "/lab-admin/records", "record"));
        shortcuts.add(shortcut("统计分析", "运行指标与趋势分析", "/lab-admin/statistics", "statistics"));
        return shortcuts;
    }

    /**
     * shortcut的处理逻辑。
     * @param title title参数。
     * @param description description参数。
     * @param path 路径参数。
     * @param iconKey icon键参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, Object> shortcut(String title, String description, String path, String iconKey) {
        Map<String, Object> shortcut = new LinkedHashMap<>();
        shortcut.put("title", title);
        shortcut.put("description", description);
        shortcut.put("path", path);
        shortcut.put("iconKey", iconKey);
        return shortcut;
    }

    /**
     * put列表的处理逻辑。
     * @param overview 概况参数。
     * @param key 键参数。
     * @param label 标签参数。
     * @param supplier supplier参数。
     * @param loadWarnings loadWarnings参数。
     */
    private void putList(
            Map<String, Object> overview,
            String key,
            String label,
            Supplier<List<Map<String, Object>>> supplier,
            List<String> loadWarnings
    ) {
        overview.put(key, safeList(label, supplier, loadWarnings));
    }

    /**
     * 安全长整数的处理逻辑。
     * @param label 标签参数。
     * @param supplier supplier参数。
     * @param loadWarnings loadWarnings参数。
     * @return long 类型的处理结果。
     */
    private long safeLong(String label, LongSupplier supplier, List<String> loadWarnings) {
        try {
            return supplier.getAsLong();
        } catch (RuntimeException exception) {
            recordLoadWarning(label, loadWarnings, exception);
            return 0L;
        }
    }

    /**
     * 安全列表的处理逻辑。
     * @param label 标签参数。
     * @param supplier supplier参数。
     * @param loadWarnings loadWarnings参数。
     * @return 当前操作得到的记录集合。
     */
    private List<Map<String, Object>> safeList(
            String label,
            Supplier<List<Map<String, Object>>> supplier,
            List<String> loadWarnings
    ) {
        try {
            return safeList(supplier.get());
        } catch (RuntimeException exception) {
            recordLoadWarning(label, loadWarnings, exception);
            return List.of();
        }
    }

    /**
     * 安全实验室列表的处理逻辑。
     * @param label 标签参数。
     * @param supplier supplier参数。
     * @param loadWarnings loadWarnings参数。
     * @return 当前操作得到的记录集合。
     */
    private List<LabEntity> safeLabList(
            String label,
            Supplier<List<LabEntity>> supplier,
            List<String> loadWarnings
    ) {
        try {
            List<LabEntity> source = supplier.get();
            return source == null ? List.of() : source;
        } catch (RuntimeException exception) {
            recordLoadWarning(label, loadWarnings, exception);
            return List.of();
        }
    }

    /**
     * 安全长整数列表的处理逻辑。
     * @param label 标签参数。
     * @param supplier supplier参数。
     * @param loadWarnings loadWarnings参数。
     * @return 当前操作得到的记录集合。
     */
    private List<Long> safeLongList(
            String label,
            Supplier<List<Long>> supplier,
            List<String> loadWarnings
    ) {
        try {
            List<Long> source = supplier.get();
            return source == null ? List.of() : source;
        } catch (RuntimeException exception) {
            recordLoadWarning(label, loadWarnings, exception);
            return List.of();
        }
    }

    /**
     * 安全映射的处理逻辑。
     * @param label 标签参数。
     * @param supplier supplier参数。
     * @param loadWarnings loadWarnings参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, Object> safeMap(
            String label,
            Supplier<Map<String, Object>> supplier,
            List<String> loadWarnings
    ) {
        try {
            Map<String, Object> source = supplier.get();
            return source == null ? Map.of() : source;
        } catch (RuntimeException exception) {
            recordLoadWarning(label, loadWarnings, exception);
            return Map.of();
        }
    }

    /**
     * 记录LoadWarning。
     * @param label 标签参数。
     * @param loadWarnings loadWarnings参数。
     * @param exception 异常参数。
     */
    private void recordLoadWarning(String label, List<String> loadWarnings, RuntimeException exception) {
        loadWarnings.add(label);
        log.warn("dashboard overview section failed section={}", label, exception);
    }

    /**
     * 安全列表的处理逻辑。
     * @param source 来源参数。
     * @return 当前操作得到的记录集合。
     */
    private List<Map<String, Object>> safeList(List<Map<String, Object>> source) {
        return source == null ? List.of() : source;
    }
}
