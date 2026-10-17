/*
 * 通知公告实现模块。
 * 主要协作组件：NoticeMapper、BusinessLoopService。
 */
package org.example.backend.service.impl;

import org.example.backend.entity.NoticeEntity;
import org.example.backend.entity.NoticeRecipientEntity;
import org.example.backend.mapper.NoticeMapper;
import org.example.backend.service.BusinessLoopService;
import org.example.backend.service.NoticeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NoticeServiceImpl implements NoticeService {
    private final NoticeMapper noticeMapper;
    private final BusinessLoopService businessLoopService;

    /**
     * 构造 NoticeServiceImpl，初始化当前实例。
     * @param noticeMapper 通知数据映射器参数。
     * @param businessLoopService businessLoop服务参数。
     */
    public NoticeServiceImpl(NoticeMapper noticeMapper, BusinessLoopService businessLoopService) {
        this.noticeMapper = noticeMapper;
        this.businessLoopService = businessLoopService;
    }

    /**
     * 列出通知。
     * @return 当前操作得到的记录集合。
     */
    @Override
    public List<NoticeEntity> listNotices() {
        return noticeMapper.listNotices();
    }

    /**
     * 创建通知。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param notice 通知参数。
     * @return NoticeEntity 类型的处理结果。
     */
    @Override
    @Transactional
    public NoticeEntity createNotice(NoticeEntity notice) {
        NoticeEntity normalizedNotice = normalizeNotice(notice);
        noticeMapper.insertNotice(normalizedNotice);
        NoticeEntity savedNotice = noticeMapper.getNotice(normalizedNotice.getId());
        refreshRecipientsIfPublished(savedNotice);
        recordNoticeEvent("create", savedNotice);
        return noticeMapper.getNotice(savedNotice.getId());
    }

    /**
     * 更新通知。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param id 标识参数。
     * @param notice 通知参数。
     * @return NoticeEntity 类型的处理结果。
     */
    @Override
    @Transactional
    public NoticeEntity updateNotice(Long id, NoticeEntity notice) {
        noticeMapper.updateNotice(id, normalizeNotice(notice));
        NoticeEntity savedNotice = noticeMapper.getNotice(id);
        refreshRecipientsIfPublished(savedNotice);
        recordNoticeEvent("update", savedNotice);
        return noticeMapper.getNotice(id);
    }

    /**
     * 删除通知。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param id 标识参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    @Transactional
    public List<NoticeEntity> deleteNotice(Long id) {
        NoticeEntity notice = noticeMapper.getNotice(id);
        noticeMapper.logicalDeleteNotice(id);
        businessLoopService.recordEvent("notice", "delete", notice == null ? String.valueOf(id) : notice.getTitle(), "已删除", Map.of("id", id));
        return noticeMapper.listNotices();
    }

    /**
     * replace通知的处理逻辑。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param notices 通知参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    @Transactional
    public List<NoticeEntity> replaceNotices(List<NoticeEntity> notices) {
        noticeMapper.deleteAllRecipients();
        noticeMapper.deleteAllNotices();
        for (NoticeEntity notice : notices == null ? Collections.<NoticeEntity>emptyList() : notices) {
            NoticeEntity normalizedNotice = normalizeNotice(notice);
            noticeMapper.insertNotice(normalizedNotice);
            refreshRecipientsIfPublished(noticeMapper.getNotice(normalizedNotice.getId()));
        }
        businessLoopService.recordEvent("notice", "batch-save", "通知公告", "已同步", Map.of("count", notices == null ? 0 : notices.size()));
        return noticeMapper.listNotices();
    }

    /**
     * 重置通知。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @return 当前操作得到的记录集合。
     */
    @Override
    @Transactional
    public List<NoticeEntity> resetNotices() {
        noticeMapper.deleteAllRecipients();
        noticeMapper.deleteAllNotices();
        for (NoticeEntity notice : getDefaultNotices()) {
            noticeMapper.insertNotice(normalizeNotice(notice));
            refreshRecipientsIfPublished(noticeMapper.getNotice(notice.getId()));
        }
        businessLoopService.recordEvent("notice", "reset", "通知公告", "已恢复初始公告", Map.of("count", getDefaultNotices().size()));
        return noticeMapper.listNotices();
    }

    /**
     * publish通知的处理逻辑。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param id 标识参数。
     * @return NoticeEntity 类型的处理结果。
     */
    @Override
    @Transactional
    public NoticeEntity publishNotice(Long id) {
        noticeMapper.publishNotice(id);
        NoticeEntity notice = noticeMapper.getNotice(id);
        refreshRecipientsIfPublished(notice);
        recordNoticeEvent("publish", notice);
        return noticeMapper.getNotice(id);
    }

    /**
     * withdraw通知的处理逻辑。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param id 标识参数。
     * @return NoticeEntity 类型的处理结果。
     */
    @Override
    @Transactional
    public NoticeEntity withdrawNotice(Long id) {
        noticeMapper.updateNoticeStatus(id, "已撤回");
        NoticeEntity notice = noticeMapper.getNotice(id);
        recordNoticeEvent("withdraw", notice);
        return notice;
    }

    /**
     * 归档通知的处理逻辑。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param id 标识参数。
     * @return NoticeEntity 类型的处理结果。
     */
    @Override
    @Transactional
    public NoticeEntity archiveNotice(Long id) {
        noticeMapper.archiveNotice(id);
        NoticeEntity notice = noticeMapper.getNotice(id);
        recordNoticeEvent("archive", notice);
        return notice;
    }

    /**
     * 列出Recipients。
     * @param noticeId 通知标识参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    public List<NoticeRecipientEntity> listRecipients(Long noticeId) {
        return noticeMapper.listRecipients(noticeId);
    }

    /**
     * 列出用户通知。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param userId 用户标识参数。
     * @param roleCode 角色编码参数。
     * @param readStatus read状态参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    @Transactional
    public List<NoticeRecipientEntity> listUserNotices(Integer userId, String roleCode, String readStatus) {
        syncUserRecipients(userId, roleCode);
        return noticeMapper.listUserNotices(userId, readStatus);
    }

    /**
     * 标记Read。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param noticeId 通知标识参数。
     * @param userId 用户标识参数。
     * @param roleCode 角色编码参数。
     * @return NoticeRecipientEntity 类型的处理结果。
     */
    @Override
    @Transactional
    public NoticeRecipientEntity markRead(Long noticeId, Integer userId, String roleCode) {
        syncUserRecipients(userId, roleCode);
        noticeMapper.markRead(noticeId, userId);
        return noticeMapper.listUserNotices(userId, null).stream()
                .filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ item -> noticeId != null && noticeId.equals(item.getNoticeId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取通知Stats。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param userId 用户标识参数。
     * @param roleCode 角色编码参数。
     * @return 当前操作返回的键值结果。
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getNoticeStats(Integer userId, String roleCode) {
        return noticeMapper.getNoticeStats(userId);
    }

    /**
     * 同步BusinessReminders。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @return 包含 labAdmin、consumable、reservation、maintenance、repair、notice、count、created 等字段的结果对象。
     */
    @Override
    @Transactional
    public Map<String, Object> syncBusinessReminders() {
        List<NoticeEntity> notices = new ArrayList<>();

        for (Map<String, Object> item : noticeMapper.listLowStockConsumables()) {
            notices.add(buildBusinessNotice(
                    String.valueOf(item.get("name")) + "库存预警",
                    "库存预警",
                    "labAdmin",
                    "consumable",
                    String.valueOf(item.get("id")),
                    "当前库存 " + item.get("stock") + firstNonBlank(String.valueOf(item.get("unit")), "件")
                            + "，低于预警阈值 " + item.get("warnThreshold") + "，请及时补货。"
            ));
        }

        for (Map<String, Object> item : noticeMapper.listPendingReservations()) {
            notices.add(buildBusinessNotice(
                    "预约审批待处理：" + firstNonBlank(String.valueOf(item.get("applicantName")), "预约申请"),
                    "预约审批",
                    "labAdmin",
                    "reservation",
                    String.valueOf(item.get("id")),
                    "申请实验室：" + firstNonBlank(String.valueOf(item.get("labName")), "-")
                            + "，时间：" + item.get("reservationDate") + " " + item.get("timeRange")
                            + "，状态：" + item.get("status") + "。"
            ));
        }

        for (Map<String, Object> item : noticeMapper.listActiveRepairs()) {
            notices.add(buildBusinessNotice(
                    "设备维修待处理：" + firstNonBlank(String.valueOf(item.get("ticket")), "维修工单"),
                    "设备维修",
                    "maintenance",
                    "repair",
                    String.valueOf(item.get("id")),
                    "设备：" + firstNonBlank(String.valueOf(item.get("deviceName")), "-")
                            + "，故障类型：" + firstNonBlank(String.valueOf(item.get("faultType")), "-")
                            + "，状态：" + item.get("status") + "。"
            ));
        }

        int publishedCount = 0;
        for (NoticeEntity notice : notices) {
            if (noticeMapper.countNoticeBySource(notice.getSourceModule(), notice.getSourceId()) > 0) {
                continue;
            }
            noticeMapper.insertNotice(normalizeNotice(notice));
            refreshRecipientsIfPublished(noticeMapper.getNotice(notice.getId()));
            publishedCount++;
        }

        businessLoopService.recordEvent("notice", "sync-business-reminders", "业务提醒", "已同步", Map.of("count", publishedCount));
        return Map.of("created", publishedCount);
    }

    /**
     * 记录通知事件。
     * @param action action参数。
     * @param notice 通知参数。
     */
    private void recordNoticeEvent(String action, NoticeEntity notice) {
        if (notice == null) {
            return;
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("id", notice.getId());
        details.put("type", firstNonBlank(notice.getNoticeType(), notice.getType()));
        details.put("target", firstNonBlank(notice.getTargetRole(), notice.getTarget()));
        businessLoopService.recordEvent("notice", action, firstNonBlank(notice.getTitle(), "通知公告"), firstNonBlank(notice.getPublishStatus(), notice.getStatus()), details);
    }

    /**
     * 刷新RecipientsIfPublished。
     * @param notice 通知参数。
     */
    private void refreshRecipientsIfPublished(NoticeEntity notice) {
        if (notice == null || notice.getId() == null || !"已发布".equals(firstNonBlank(notice.getPublishStatus(), notice.getStatus()))) {
            return;
        }

        noticeMapper.deleteRecipientsByNotice(notice.getId());
        noticeMapper.insertRecipientsForRole(notice.getId(), normalizeTargetRole(firstNonBlank(notice.getTargetRole(), notice.getTarget())));
    }

    /**
     * 同步用户Recipients。
     * @param userId 用户标识参数。
     * @param roleCode 角色编码参数。
     */
    private void syncUserRecipients(Integer userId, String roleCode) {
        if (userId == null) {
            return;
        }

        noticeMapper.syncUserRecipients(userId, normalizeTargetRole(roleCode));
    }

    /**
     * 归一化通知。
     * @param notice 通知参数。
     * @return NoticeEntity 类型的处理结果。
     */
    private NoticeEntity normalizeNotice(NoticeEntity notice) {
        NoticeEntity normalizedNotice = notice == null ? new NoticeEntity() : notice;
        if (isBlank(normalizedNotice.getTitle())) {
            normalizedNotice.setTitle("系统提醒");
        }
        if (isBlank(normalizedNotice.getNoticeType())) {
            normalizedNotice.setNoticeType(normalizedNotice.getType());
        }
        if (isBlank(normalizedNotice.getType())) {
            normalizedNotice.setType(normalizedNotice.getNoticeType());
        }
        if (isBlank(normalizedNotice.getType())) {
            normalizedNotice.setType("系统提醒");
            normalizedNotice.setNoticeType("系统提醒");
        }
        if (isBlank(normalizedNotice.getTargetRole())) {
            normalizedNotice.setTargetRole(normalizedNotice.getTarget());
        }
        if (isBlank(normalizedNotice.getTarget())) {
            normalizedNotice.setTarget(normalizedNotice.getTargetRole());
        }
        if (isBlank(normalizedNotice.getTarget())) {
            normalizedNotice.setTarget("labAdmin");
            normalizedNotice.setTargetRole("labAdmin");
        }
        normalizedNotice.setTargetRole(normalizeTargetRole(normalizedNotice.getTargetRole()));
        normalizedNotice.setTarget(normalizedNotice.getTargetRole());
        if (isBlank(normalizedNotice.getPriority())) {
            normalizedNotice.setPriority("普通");
        }
        if (isBlank(normalizedNotice.getPublishStatus())) {
            normalizedNotice.setPublishStatus(normalizedNotice.getStatus());
        }
        if (isBlank(normalizedNotice.getStatus())) {
            normalizedNotice.setStatus(normalizedNotice.getPublishStatus());
        }
        if (isBlank(normalizedNotice.getStatus())) {
            normalizedNotice.setStatus("草稿");
            normalizedNotice.setPublishStatus("草稿");
        }
        if (isBlank(normalizedNotice.getContent())) {
            normalizedNotice.setContent(normalizedNotice.getTitle());
        }
        if ("已发布".equals(firstNonBlank(normalizedNotice.getStatus(), normalizedNotice.getPublishStatus()))
                && normalizedNotice.getPublishTime() == null) {
            normalizedNotice.setPublishTime(LocalDateTime.now());
        }
        return normalizedNotice;
    }

    /**
     * 构建Business通知。
     * @param title title参数。
     * @param type 类型参数。
     * @param targetRole 目标角色参数。
     * @param sourceModule 来源Module参数。
     * @param sourceId 来源标识参数。
     * @param content 内容参数。
     * @return NoticeEntity 类型的处理结果。
     */
    private NoticeEntity buildBusinessNotice(String title, String type, String targetRole, String sourceModule, String sourceId, String content) {
        NoticeEntity notice = new NoticeEntity();
        notice.setTitle(title);
        notice.setType(type);
        notice.setNoticeType(type);
        notice.setTarget(targetRole);
        notice.setTargetRole(targetRole);
        notice.setPriority("高");
        notice.setSourceModule(sourceModule);
        notice.setSourceId(sourceId);
        notice.setBusinessType(type);
        notice.setContent(content);
        notice.setStatus("已发布");
        notice.setPublishStatus("已发布");
        notice.setPublishTime(LocalDateTime.now());
        return notice;
    }

    /**
     * 获取默认通知。
     * @return 当前操作得到的记录集合。
     */
    private List<NoticeEntity> getDefaultNotices() {
        return List.of(
                buildNotice("课程环境确认提醒", "环境确认", "teacher", "请任课教师确认已配置课程环境。", "已发布"),
                buildNotice("设备维护提醒", "设备维护", "labAdmin", "请关注待处理设备维护工单。", "已发布"),
                buildNotice("库存预警提醒", "库存预警", "labAdmin", "请及时处理低库存耗材。", "草稿")
        );
    }

    /**
     * 构建通知。
     * @param title title参数。
     * @param type 类型参数。
     * @param target 目标参数。
     * @param content 内容参数。
     * @param status 状态参数。
     * @return NoticeEntity 类型的处理结果。
     */
    private NoticeEntity buildNotice(String title, String type, String target, String content, String status) {
        NoticeEntity notice = new NoticeEntity();
        notice.setTitle(title);
        notice.setType(type);
        notice.setNoticeType(type);
        notice.setTarget(target);
        notice.setTargetRole(target);
        notice.setContent(content);
        notice.setStatus(status);
        notice.setPublishStatus(status);
        if ("已发布".equals(status)) {
            notice.setPublishTime(LocalDateTime.now());
        }
        return notice;
    }

    /**
     * 判断是否为空白。
     * @param value 值参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * first非空白的处理逻辑。
     * @return String 类型的处理结果。
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return "";
    }

    /**
     * 归一化目标角色。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String normalizeTargetRole(String value) {
        String text = firstNonBlank(value, "labAdmin");
        if ("all".equalsIgnoreCase(text) || text.contains("全部")) {
            return "all";
        }
        if ("systemAdmin".equalsIgnoreCase(text) || text.contains("系统") || text.contains("平台")) {
            return "systemAdmin";
        }
        if ("teacher".equalsIgnoreCase(text) || text.contains("教师") || text.contains("任课") || text.contains("业务使用方")) {
            return "teacher";
        }
        if ("maintenance".equalsIgnoreCase(text) || text.contains("维修") || text.contains("运维")) {
            return "maintenance";
        }
        if ("labAdmin".equalsIgnoreCase(text) || text.contains("实验室")) {
            return "labAdmin";
        }
        return text;
    }
}
