package org.example.backend.service.impl;

import org.example.backend.entity.LabEntity;
import org.example.backend.entity.ReservationsEntity;
import org.example.backend.mapper.ClassTimetableMapper;
import org.example.backend.mapper.LabMapper;
import org.example.backend.mapper.ReservationsMapper;
import org.example.backend.service.BusinessLoopService;
import org.example.backend.service.ReservationsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 维护预约状态、时间冲突及实验室使用状态；写入流程同时记录业务闭环事件。
 * 更新与审核应通过本服务，避免直接写 Mapper 跳过状态流转规则。
 */
@Service
public class ReservationsServiceImpl implements ReservationsService {
    private static final String CRAWLER_CONFIG_KEY = "lab-admin-class-timetable";
    private static final String STATUS_PENDING = "待审核";
    private static final String STATUS_APPROVED = "已通过";
    private static final String STATUS_REJECTED = "已驳回";
    private static final String STATUS_CANCELLED = "已取消";
    private static final String STATUS_EXPIRED = "已过期";
    private static final String STATUS_CONFLICT = "冲突";
    private static final String STATUS_COMPLETED = "已完成";
    private static final Set<String> ALLOWED_SCENES = Set.of("课程", "自主", "考试", "竞赛");

    private final ReservationsMapper reservationsMapper;
    private final LabMapper labMapper;
    private final ClassTimetableMapper classTimetableMapper;
    private final BusinessLoopService businessLoopService;

    /**
     * 构造 ReservationsServiceImpl，初始化当前实例。
     * @param reservationsMapper 预约数据映射器参数。
     * @param labMapper 实验室数据映射器参数。
     * @param classTimetableMapper class课表数据映射器参数。
     * @param businessLoopService businessLoop服务参数。
     */
    public ReservationsServiceImpl(
            ReservationsMapper reservationsMapper,
            LabMapper labMapper,
            ClassTimetableMapper classTimetableMapper,
            BusinessLoopService businessLoopService
    ) {
        this.reservationsMapper = reservationsMapper;
        this.labMapper = labMapper;
        this.classTimetableMapper = classTimetableMapper;
        this.businessLoopService = businessLoopService;
    }

    /**
     * 获取预约。
     * @return Object 类型的处理结果。
     */
    @Override
    public Object getReservations() {
        refreshExpiredReservations();
        return reservationsMapper.getReservations();
    }

    /**
     * 插入预约。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param reservation 预约参数。
     */
    @Override
    @Transactional
    public void insertReservation(ReservationsEntity reservation) {
        normalizeReservation(null, reservation);
        reservationsMapper.insertReservation(reservation);
        businessLoopService.recordEvent("reservation", "create", reservation.getLab(), reservation.getStatus(), Map.of(
                "reservationId", reservation.getId(),
                "applicant", reservation.getApplicant()
        ));
    }

    /**
     * 获取预约。
     * @param id 标识参数。
     * @return Object 类型的处理结果。
     */
    @Override
    public Object getReservation(Integer id) {
        refreshExpiredReservations();
        return reservationsMapper.getReservation(id);
    }

    /**
     * 更新预约。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param id 标识参数。
     * @param reservation 预约参数。
     */
    @Override
    @Transactional
    public void updateReservation(Integer id, ReservationsEntity reservation) {
        ReservationsEntity previousReservation = reservationsMapper.getReservation(id);
        if (previousReservation == null) {
            throw new IllegalArgumentException("预约记录不存在");
        }

        validateStatusTransitionForUpdate(previousReservation, reservation);
        normalizeReservation(id, reservation);
        int updatedCount = reservationsMapper.updateReservation(id, reservation);
        if (updatedCount == 0) {
            throw new IllegalArgumentException("预约记录不存在");
        }

        ReservationsEntity updatedReservation = reservationsMapper.getReservation(id);
        refreshLabStatus(previousReservation.getLabId());
        refreshLabStatus(updatedReservation.getLabId());
        businessLoopService.recordEvent("reservation", "update", updatedReservation.getLab(), updatedReservation.getStatus(), Map.of(
                "reservationId", id
        ));
    }

    /**
     * 删除预约。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param id 标识参数。
     */
    @Override
    @Transactional
    public void deleteReservation(Integer id) {
        ReservationsEntity reservation = reservationsMapper.getReservation(id);
        reservationsMapper.deleteReservation(id);
        if (reservation != null) {
            refreshLabStatus(reservation.getLabId());
        }
        businessLoopService.recordEvent("reservation", "delete", String.valueOf(id), "已删除", Map.of("reservationId", id));
    }

    /**
     * replace预约的处理逻辑。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param reservations 预约参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    @Transactional
    /** 非空列表按全量台账替换；空列表仅查询，不能用此入口清空预约记录。 */
    public List<ReservationsEntity> replaceReservations(List<ReservationsEntity> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            return reservationsMapper.getReservations();
        }

        reservationsMapper.deleteAllReservations();
        for (ReservationsEntity reservation : reservations) {
            normalizeReservation(null, reservation);
            reservationsMapper.insertReservation(reservation);
        }
        businessLoopService.recordEvent("reservation", "batch-save", "预约台账", "已同步", Map.of("count", reservations.size()));

        refreshExpiredReservations();
        return reservationsMapper.getReservations();
    }

    /**
     * 获取Approved。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param id 标识参数。
     * @param reservationPatch 预约Patch参数。
     * @return ReservationsEntity 类型的处理结果。
     */
    @Override
    @Transactional
    public ReservationsEntity getApproved(Integer id, ReservationsEntity reservationPatch) {
        normalizeTextFields(reservationPatch);
        validateOptionalMeaningfulText("审核备注", reservationPatch.getReviewRemark(), 4, 120);
        refreshExpiredReservations();
        ReservationsEntity currentReservation = reservationsMapper.getReservation(id);
        if (currentReservation == null) {
            throw new IllegalArgumentException("预约记录不存在");
        }
        if (STATUS_CONFLICT.equals(currentReservation.getStatus()) || Boolean.TRUE.equals(currentReservation.getConflict())) {
            throw new IllegalArgumentException("该预约存在时间冲突，无法通过");
        }
        if (STATUS_EXPIRED.equals(currentReservation.getStatus())
                || (STATUS_PENDING.equals(currentReservation.getStatus()) && isPastReservationStart(currentReservation))) {
            expireReservation(currentReservation);
            throw new IllegalArgumentException("该预约已过期，无法通过");
        }
        if (!STATUS_PENDING.equals(currentReservation.getStatus())) {
            throw new IllegalArgumentException("只能通过待审核预约");
        }
        List<String> availabilityIssues = collectAvailabilityIssues(id, currentReservation, true);
        if (!availabilityIssues.isEmpty()) {
            throw new IllegalArgumentException("该预约存在时间冲突，无法通过");
        }
        int updatedCount = reservationsMapper.getApproved(id, reservationPatch);
        if (updatedCount == 0) {
            throw new IllegalArgumentException("预约记录不存在");
        }
        ReservationsEntity reservation = reservationsMapper.getReservation(id);
        businessLoopService.syncUsageRecordAfterReservationApproved(reservation);
        refreshLabStatus(reservation.getLabId());
        return reservation;
    }

    /**
     * 获取Rejected。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param id 标识参数。
     * @param reservationPatch 预约Patch参数。
     * @return ReservationsEntity 类型的处理结果。
     */
    @Override
    @Transactional
    public ReservationsEntity getRejected(Integer id, ReservationsEntity reservationPatch) {
        normalizeTextFields(reservationPatch);
        validateMeaningfulText("驳回原因", reservationPatch.getReviewRemark(), 4, 120);
        refreshExpiredReservations();
        ReservationsEntity currentReservation = reservationsMapper.getReservation(id);
        if (currentReservation == null) {
            throw new IllegalArgumentException("预约记录不存在");
        }
        if (STATUS_EXPIRED.equals(currentReservation.getStatus())
                || (STATUS_PENDING.equals(currentReservation.getStatus()) && isPastReservationStart(currentReservation))) {
            expireReservation(currentReservation);
            throw new IllegalArgumentException("已过期预约不能驳回");
        }
        if (!STATUS_PENDING.equals(currentReservation.getStatus()) && !STATUS_CONFLICT.equals(currentReservation.getStatus())) {
            throw new IllegalArgumentException("只能驳回待审核或冲突预约");
        }
        int updatedCount = reservationsMapper.getRejected(id, reservationPatch);
        if (updatedCount == 0) {
            throw new IllegalArgumentException("预约记录不存在");
        }
        ReservationsEntity reservation = reservationsMapper.getReservation(id);
        refreshLabStatus(reservation.getLabId());
        businessLoopService.recordEvent("reservation", "reject", reservation.getLab(), reservation.getStatus(), Map.of(
                "reservationId", id,
                "reason", firstNonBlank(reservation.getNote(), "驳回")
        ));
        return reservation;
    }

    /**
     * 获取Scan预约Profile。
     * @param labId 实验室标识参数。
     * @param labCode 实验室编码参数。
     * @return 当前操作返回的键值结果。
     */
    @Override
    public Map<String, Object> getScanReservationProfile(Integer labId, String labCode) {
        LabEntity lab = resolveLab(labId == null ? null : labId.longValue(), labCode, null);
        if (lab == null) {
            throw new IllegalArgumentException("实验室信息不存在");
        }
        if (!isBlank(labCode) && !labCode.equalsIgnoreCase(firstNonBlank(lab.getLabCode(), ""))) {
            throw new IllegalArgumentException("二维码与实验室信息不匹配");
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", lab.getId());
        profile.put("code", lab.getLabCode());
        profile.put("name", lab.getLabName());
        profile.put("building", lab.getBuilding());
        profile.put("floor", lab.getFloor());
        profile.put("room", lab.getRoomNo());
        profile.put("type", lab.getLabType());
        profile.put("capacity", lab.getCapacity());
        profile.put("manager", lab.getManagerName());
        profile.put("status", lab.getOpenStatus());
        profile.put("displayName", formatLabDisplayName(lab));
        return profile;
    }

    /**
     * 提交Scan预约。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param reservation 预约参数。
     * @return ReservationsEntity 类型的处理结果。
     */
    @Override
    @Transactional
    public ReservationsEntity submitScanReservation(ReservationsEntity reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("请填写预约信息");
        }

        prepareScanReservation(reservation);
        List<String> availabilityIssues = collectAvailabilityIssues(null, reservation, true);
        if (!availabilityIssues.isEmpty()) {
            throw new IllegalArgumentException(String.join("；", availabilityIssues));
        }

        reservationsMapper.insertReservation(reservation);
        ReservationsEntity savedReservation = reservationsMapper.getReservation(reservation.getId().intValue());
        businessLoopService.recordEvent("reservation", "scan-create", savedReservation.getLab(), savedReservation.getStatus(), Map.of(
                "reservationId", savedReservation.getId(),
                "applicant", savedReservation.getApplicant()
        ));
        return savedReservation;
    }

    /**
     * 获取Scan预约状态。
     * @param id 标识参数。
     * @param contact contact参数。
     * @return 当前操作返回的键值结果。
     */
    @Override
    public Map<String, Object> getScanReservationStatus(Integer id, String contact) {
        if (id == null) {
            throw new IllegalArgumentException("请填写预约编号");
        }
        if (isBlank(contact)) {
            throw new IllegalArgumentException("请填写提交时的联系方式");
        }

        ReservationsEntity reservation = reservationsMapper.getReservation(id);
        if (reservation == null || !normalizeToken(contact).equals(normalizeToken(reservation.getContact()))) {
            throw new IllegalArgumentException("预约编号或联系方式不匹配");
        }

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("id", reservation.getId());
        status.put("status", reservation.getStatus());
        status.put("tagType", reservation.getTagType());
        status.put("applicant", reservation.getApplicant());
        status.put("department", reservation.getDepartment());
        status.put("lab", reservation.getLab());
        status.put("date", reservation.getDate());
        status.put("timeRange", reservation.getTimeRange());
        status.put("scene", reservation.getScene());
        status.put("attendees", reservation.getAttendees());
        status.put("reviewer", reservation.getReviewer());
        status.put("submittedAt", reservation.getSubmittedAt());
        status.put("reviewedAt", reservation.getReviewedAt());
        status.put("note", reservation.getNote());
        return status;
    }

    /**
     * 归一化预约。
     * @param id 标识参数。
     * @param reservation 预约参数。
     */
    private void normalizeReservation(Integer id, ReservationsEntity reservation) {
        normalizeTextFields(reservation);
        validateApplicantFields(reservation, false);
        validateRequiredFields(reservation);
        resolveReservationLab(reservation);

        if ("加签中".equals(reservation.getStatus())) {
            reservation.setStatus(STATUS_PENDING);
        }
        if (reservation.getStatus() == null || reservation.getStatus().isBlank()) {
            reservation.setStatus(STATUS_PENDING);
        }
        if (reservation.getReviewerName() == null || reservation.getReviewerName().isBlank()) {
            reservation.setReviewerName(STATUS_PENDING);
        }

        if (STATUS_PENDING.equals(reservation.getStatus()) && isPastReservationStart(reservation)) {
            reservation.setStatus(STATUS_EXPIRED);
            reservation.setConflict(false);
            reservation.setReviewerName(firstNonBlank(reservation.getReviewerName(), "系统"));
            if (isBlank(reservation.getReviewRemark())) {
                reservation.setReviewRemark("预约已过期。");
            }
            return;
        }

        if (STATUS_REJECTED.equals(reservation.getStatus())
                || STATUS_CANCELLED.equals(reservation.getStatus())
                || STATUS_EXPIRED.equals(reservation.getStatus())
                || STATUS_COMPLETED.equals(reservation.getStatus())) {
            reservation.setConflict(false);
            return;
        }

        List<String> availabilityIssues = collectAvailabilityIssues(id, reservation, false);
        reservation.setConflict(!availabilityIssues.isEmpty());
        if (Boolean.TRUE.equals(reservation.getConflict()) && STATUS_PENDING.equals(reservation.getStatus())) {
            reservation.setStatus(STATUS_CONFLICT);
            if (reservation.getReviewRemark() == null || reservation.getReviewRemark().isBlank()) {
                reservation.setReviewRemark(String.join("；", availabilityIssues));
            }
        }
    }

    /**
     * 准备Scan预约。
     * @param reservation 预约参数。
     */
    private void prepareScanReservation(ReservationsEntity reservation) {
        normalizeTextFields(reservation);
        validateApplicantFields(reservation, true);
        validateRequiredFields(reservation);
        if (reservation.getReservationDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("预约日期不能早于今天");
        }
        resolveReservationLab(reservation);

        reservation.setStatus(STATUS_PENDING);
        reservation.setConflict(false);
        reservation.setReviewerName(STATUS_PENDING);
        if (isBlank(reservation.getReviewRemark())) {
            reservation.setReviewRemark("扫码提交，待管理员审核。");
        }
    }

    /**
     * 刷新Expired预约。
     */
    private void refreshExpiredReservations() {
        List<ReservationsEntity> reservations = reservationsMapper.getReservations();
        for (ReservationsEntity reservation : reservations) {
            if ("加签中".equals(reservation.getStatus())) {
                reservationsMapper.updateReviewStatus(
                        reservation.getId().intValue(),
                        STATUS_PENDING,
                        STATUS_PENDING,
                        firstNonBlank(reservation.getReviewRemark(), "加签状态已取消，返回待审核")
                );
                reservation.setStatus(STATUS_PENDING);
            }

            if (STATUS_PENDING.equals(reservation.getStatus()) && isPastReservationStart(reservation)) {
                expireReservation(reservation);
            }
        }
    }

    /**
     * expire预约的处理逻辑。
     * @param reservation 预约参数。
     */
    private void expireReservation(ReservationsEntity reservation) {
        if (reservation == null || reservation.getId() == null || STATUS_EXPIRED.equals(reservation.getStatus())) {
            return;
        }
        reservationsMapper.updateReviewStatus(
                reservation.getId().intValue(),
                STATUS_EXPIRED,
                "系统",
                "预约已过期。"
        );
        reservation.setStatus(STATUS_EXPIRED);
        reservation.setReviewerName("系统");
        reservation.setReviewRemark("预约已过期。");
        reservation.setConflict(false);
    }

    /**
     * 校验状态TransitionForUpdate。
     * @param previousReservation previous预约参数。
     * @param nextReservation next预约参数。
     */
    private void validateStatusTransitionForUpdate(ReservationsEntity previousReservation, ReservationsEntity nextReservation) {
        if (previousReservation == null || nextReservation == null) {
            return;
        }

        String nextStatus = firstNonBlank(nextReservation.getStatus(), previousReservation.getStatus());
        String previousStatus = "加签中".equals(previousReservation.getStatus()) ? STATUS_PENDING : previousReservation.getStatus();

        if (STATUS_CANCELLED.equals(nextStatus)) {
            if (!STATUS_PENDING.equals(previousStatus) && !STATUS_APPROVED.equals(previousStatus) && !STATUS_CONFLICT.equals(previousStatus)) {
                throw new IllegalArgumentException("当前状态不能取消预约");
            }
            if (isPastReservationStart(previousReservation)) {
                if (STATUS_PENDING.equals(previousStatus)) {
                    expireReservation(previousReservation);
                }
                throw new IllegalArgumentException("当前状态不能取消预约");
            }
            if (isBlank(nextReservation.getReviewRemark())) {
                throw new IllegalArgumentException("请填写取消原因");
            }
            return;
        }

        if (STATUS_EXPIRED.equals(previousStatus)
                || STATUS_REJECTED.equals(previousStatus)
                || STATUS_CANCELLED.equals(previousStatus)
                || STATUS_COMPLETED.equals(previousStatus)) {
            throw new IllegalArgumentException("当前状态不能编辑");
        }
    }

    /**
     * 判断是否为Past预约开始。
     * @param reservation 预约参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isPastReservationStart(ReservationsEntity reservation) {
        if (reservation == null || reservation.getReservationDate() == null || isBlank(reservation.getTimeRange())) {
            return false;
        }

        try {
            String startTime = reservation.getTimeRange().split("-")[0].trim();
            LocalTime start = LocalTime.parse(startTime);
            LocalDate today = LocalDate.now();
            if (reservation.getReservationDate().isBefore(today)) {
                return true;
            }
            return reservation.getReservationDate().isEqual(today) && !LocalTime.now().isBefore(start);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * 收集AvailabilityIssues。
     * @param id 标识参数。
     * @param reservation 预约参数。
     * @param includeCapacity includeCapacity参数。
     * @return 当前操作得到的记录集合。
     */
    private List<String> collectAvailabilityIssues(Integer id, ReservationsEntity reservation, boolean includeCapacity) {
        validateRequiredFields(reservation);
        LabEntity lab = resolveReservationLab(reservation);
        List<String> issues = new ArrayList<>();

        if (reservationsMapper.countConflictingReservations(id, reservation) > 0) {
            issues.add("所选时段已有预约记录，请更换实验室、日期或时段");
        }

        Integer week = resolveAcademicWeek(reservation.getReservationDate());
        String weekday = resolveWeekday(reservation.getReservationDate());
        if (week != null && weekday != null && reservationsMapper.countTimetableConflicts(reservation, week, weekday) > 0) {
            issues.add("所选时段已有课表占用，请更换实验室、日期或时段");
        }

        if (includeCapacity) {
            int capacity = lab.getCapacity() == null ? 0 : lab.getCapacity();
            int attendeeCount = reservation.getAttendeeCount() == null ? 1 : reservation.getAttendeeCount();
            if (capacity > 0 && attendeeCount > capacity) {
                issues.add("预约人数不能超过实验室容量 " + capacity + " 人");
            }
        }

        return issues;
    }

    /**
     * 校验RequiredFields。
     * @param reservation 预约参数。
     */
    private void validateRequiredFields(ReservationsEntity reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("请填写预约信息");
        }
        normalizeTextFields(reservation);
        if (reservation.getReservationDate() == null) {
            throw new IllegalArgumentException("请选择预约日期");
        }
        if (!isValidTimeRange(reservation.getTimeRange())) {
            throw new IllegalArgumentException("请选择有效的预约时段");
        }
        if (isBlank(reservation.getScene())) {
            throw new IllegalArgumentException("请选择预约类型");
        }
        if (!ALLOWED_SCENES.contains(reservation.getScene())) {
            throw new IllegalArgumentException("预约类型仅支持课程、自主、考试或竞赛");
        }
        if (reservation.getAttendeeCount() == null || reservation.getAttendeeCount() < 1) {
            reservation.setAttendeeCount(1);
        }
        validateMeaningfulText("预约用途", reservation.getReason(), 4, 160);
        validateOptionalMeaningfulText("补充说明", reservation.getReviewRemark(), 4, 120);
    }

    /**
     * 校验ApplicantFields。
     * @param reservation 预约参数。
     * @param strictContact strictContact参数。
     */
    private void validateApplicantFields(ReservationsEntity reservation, boolean strictContact) {
        validateMeaningfulText("申请人姓名", reservation.getApplicantName(), 2, 50);
        validateOptionalMeaningfulText("学院、班级或组织", reservation.getDepartment(), 2, 80);
        if (strictContact && isBlank(reservation.getDepartment())) {
            throw new IllegalArgumentException("请填写学院、班级或组织");
        }
        validateContact(strictContact ? "联系方式" : "联系人", reservation.getContact(), strictContact);
    }

    /**
     * 归一化文本Fields。
     * @param reservation 预约参数。
     */
    private void normalizeTextFields(ReservationsEntity reservation) {
        if (reservation == null) {
            return;
        }
        reservation.setApplicantName(trimToNull(reservation.getApplicantName()));
        reservation.setDepartment(trimToNull(reservation.getDepartment()));
        reservation.setContact(trimToNull(reservation.getContact()));
        reservation.setLabName(trimToNull(reservation.getLabName()));
        reservation.setLabCode(trimToNull(reservation.getLabCode()));
        reservation.setTimeRange(trimToNull(reservation.getTimeRange()));
        reservation.setScene(trimToNull(reservation.getScene()));
        reservation.setReason(trimToNull(reservation.getReason()));
        reservation.setReviewRemark(trimToNull(reservation.getReviewRemark()));
    }

    /**
     * 校验Meaningful文本。
     * @param label 标签参数。
     * @param value 值参数。
     * @param minLength min长度参数。
     * @param maxLength max长度参数。
     */
    private void validateMeaningfulText(String label, String value, int minLength, int maxLength) {
        if (isBlank(value)) {
            throw new IllegalArgumentException("请填写" + label);
        }
        if (hasUnsafeContent(value)) {
            throw new IllegalArgumentException(label + "不能包含脚本或 HTML 标签");
        }
        if (value.length() < minLength) {
            throw new IllegalArgumentException(label + "至少填写 " + minLength + " 个字符");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(label + "不能超过 " + maxLength + " 个字符");
        }
        if (!hasMeaningfulText(value)) {
            throw new IllegalArgumentException(label + "不能只填写数字或符号");
        }
    }

    /**
     * 校验OptionalMeaningful文本。
     * @param label 标签参数。
     * @param value 值参数。
     * @param minLength min长度参数。
     * @param maxLength max长度参数。
     */
    private void validateOptionalMeaningfulText(String label, String value, int minLength, int maxLength) {
        if (isBlank(value)) {
            return;
        }
        validateMeaningfulText(label, value, minLength, maxLength);
    }

    /**
     * 校验Contact。
     * @param label 标签参数。
     * @param value 值参数。
     * @param strict strict参数。
     */
    private void validateContact(String label, String value, boolean strict) {
        if (isBlank(value)) {
            throw new IllegalArgumentException("请填写" + label);
        }
        if (hasUnsafeContent(value)) {
            throw new IllegalArgumentException(label + "不能包含脚本或 HTML 标签");
        }
        if (value.length() > 80) {
            throw new IllegalArgumentException(label + "不能超过 80 个字符");
        }
        if (isMobile(value) || isEmail(value) || isLandline(value)) {
            return;
        }
        if (!strict && hasMeaningfulText(value) && value.length() >= 2) {
            return;
        }
        throw new IllegalArgumentException(strict
                ? label + "请填写正确的手机号、邮箱或座机"
                : label + "请填写姓名、手机号、邮箱或座机");
    }

    /**
     * 判断是否具有Unsafe内容。
     * @param value 值参数。
     * @return 当前判断的布尔结果。
     */
    private boolean hasUnsafeContent(String value) {
        return value != null && value.matches("(?i).*(<\\s*script|javascript:|on\\w+\\s*=|<[^>]+>).*");
    }

    /**
     * 判断是否具有Meaningful文本。
     * @param value 值参数。
     * @return 当前判断的布尔结果。
     */
    private boolean hasMeaningfulText(String value) {
        return value != null && value.matches(".*[\\p{IsHan}A-Za-z].*");
    }

    /**
     * 判断是否为Mobile。
     * @param value 值参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isMobile(String value) {
        String compactValue = value == null ? "" : value.replaceAll("\\s+", "");
        return compactValue.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 判断是否为Landline。
     * @param value 值参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isLandline(String value) {
        String compactValue = value == null ? "" : value.replaceAll("\\s+", "");
        return compactValue.matches("^0\\d{2,3}-?\\d{7,8}(-?\\d{1,6})?$");
    }

    /**
     * 判断是否为邮箱。
     * @param value 值参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isEmail(String value) {
        return value != null && value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    /**
     * 裁剪目标Null。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    /**
     * 解析预约实验室。
     * @param reservation 预约参数。
     * @return LabEntity 类型的处理结果。
     */
    private LabEntity resolveReservationLab(ReservationsEntity reservation) {
        LabEntity lab = resolveLab(reservation.getLabId(), reservation.getLabCode(), reservation.getLabName());
        if (lab == null) {
            throw new IllegalArgumentException("实验室信息不存在");
        }

        reservation.setLabId(lab.getId().longValue());
        reservation.setLabCode(lab.getLabCode());
        reservation.setLabName(lab.getLabName());
        return lab;
    }

    /**
     * 刷新实验室状态。
     * @param labId 实验室标识参数。
     */
    private void refreshLabStatus(Long labId) {
        if (labId == null) {
            return;
        }

        String nextStatus;
        if (reservationsMapper.countCurrentApprovedReservationsByLabId(labId) > 0) {
            nextStatus = "使用中";
        } else if (reservationsMapper.countUpcomingApprovedReservationsByLabId(labId) > 0) {
            nextStatus = "预约中";
        } else {
            LabEntity lab = labMapper.getLabById(labId.intValue());
            String currentStatus = lab == null ? "" : firstNonBlank(lab.getOpenStatus(), "");
            nextStatus = "维护中".equals(currentStatus) || "停用".equals(currentStatus) ? currentStatus : "开放";
        }

        labMapper.updateLabOpenStatus(labId.intValue(), nextStatus);
    }

    /**
     * 解析实验室。
     * @param labId 实验室标识参数。
     * @param labCode 实验室编码参数。
     * @param labName 实验室名称参数。
     * @return LabEntity 类型的处理结果。
     */
    private LabEntity resolveLab(Long labId, String labCode, String labName) {
        LabEntity lab = null;
        if (labId != null) {
            lab = labMapper.getLabById(labId.intValue());
        }

        if (lab != null) {
            return lab;
        }

        String normalizedCode = normalizeToken(firstNonBlank(labCode, extractLeadingCode(labName)));
        String normalizedName = normalizeToken(labName);

        if (isBlank(normalizedCode) && isBlank(normalizedName)) {
            return null;
        }

        return labMapper.getLabs()
                .stream()
                .filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ item -> {
                    String itemCode = normalizeToken(item.getLabCode());
                    String itemName = normalizeToken(item.getLabName());
                    String itemDisplayName = normalizeToken(formatLabDisplayName(item));
                    return (!isBlank(normalizedCode) && normalizedCode.equals(itemCode))
                            || (!isBlank(normalizedName) && (normalizedName.equals(itemName) || normalizedName.equals(itemDisplayName)));
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析教务周次。
     * @param reservationDate 预约日期参数。
     * @return Integer 类型的处理结果。
     */
    private Integer resolveAcademicWeek(LocalDate reservationDate) {
        if (reservationDate == null) {
            return null;
        }

        try {
            Map<String, Object> crawlerConfig = classTimetableMapper.getCrawlerConfig(CRAWLER_CONFIG_KEY);
            Object configuredStartDate = crawlerConfig == null ? null : crawlerConfig.get("semesterStartDate");
            if (configuredStartDate == null || String.valueOf(configuredStartDate).isBlank()) {
                return null;
            }
            LocalDate semesterStartDate = LocalDate.parse(String.valueOf(configuredStartDate));

            long diffDays = ChronoUnit.DAYS.between(semesterStartDate, reservationDate);
            if (diffDays < 0) {
                return null;
            }
            return Math.min(25, (int) (diffDays / 7) + 1);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /**
     * 解析Weekday。
     * @param reservationDate 预约日期参数。
     * @return String 类型的处理结果。
     */
    private String resolveWeekday(LocalDate reservationDate) {
        if (reservationDate == null) {
            return null;
        }

        return switch (reservationDate.getDayOfWeek().getValue()) {
            case 1 -> "星期一";
            case 2 -> "星期二";
            case 3 -> "星期三";
            case 4 -> "星期四";
            case 5 -> "星期五";
            case 6 -> "星期六";
            case 7 -> "星期日";
            default -> null;
        };
    }

    /**
     * 判断是否为有效时间范围。
     * @param timeRange 时间区间参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isValidTimeRange(String timeRange) {
        if (isBlank(timeRange) || !timeRange.matches("^\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}$")) {
            return false;
        }

        try {
            String[] parts = timeRange.split("-");
            return LocalTime.parse(parts[0]).isBefore(LocalTime.parse(parts[1]));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * 格式化实验室Display名称。
     * @param lab 实验室参数。
     * @return String 类型的处理结果。
     */
    private String formatLabDisplayName(LabEntity lab) {
        String code = firstNonBlank(lab.getLabCode(), "");
        String name = firstNonBlank(lab.getLabName(), "");
        if (isBlank(name)) {
            return code;
        }
        if (isBlank(code) || name.toLowerCase().startsWith(code.toLowerCase())) {
            return name;
        }
        return code + " " + name;
    }

    /**
     * 提取Leading编码。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String extractLeadingCode(String value) {
        if (isBlank(value)) {
            return "";
        }
        String text = value.trim();
        int spaceIndex = text.indexOf(' ');
        return spaceIndex > 0 ? text.substring(0, spaceIndex) : text;
    }

    /**
     * 归一化令牌。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "").toLowerCase();
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
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
