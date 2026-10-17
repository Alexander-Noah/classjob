package org.example.backend.service.impl;

import org.example.backend.entity.TeacherHostEntity;
import org.example.backend.mapper.TeacherHostMapper;
import org.example.backend.service.TeacherHostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/** 按实验室登记教师主机心跳；查询时根据最后心跳时间识别过期的在线状态。 */
@Service
public class TeacherHostServiceImpl implements TeacherHostService {
    // 云端教师主机判定阈值，与 Rust 网关的学生设备离线阈值不是同一配置。
    private static final int DEFAULT_OFFLINE_SECONDS = 15;
    static final String LAB_ID_REQUIRED = "labId must be positive";

    private final TeacherHostMapper teacherHostMapper;
    private final int offlineSeconds;

    /**
     * 构造 TeacherHostServiceImpl，初始化当前实例。
     * @param teacherHostMapper 教师主机数据映射器参数。
     */
    @Autowired
    public TeacherHostServiceImpl(TeacherHostMapper teacherHostMapper) {
        this(teacherHostMapper, DEFAULT_OFFLINE_SECONDS);
    }

    /**
     * 构造 TeacherHostServiceImpl，初始化当前实例。
     * @param teacherHostMapper 教师主机数据映射器参数。
     * @param offlineSeconds offlineSeconds参数。
     */
    TeacherHostServiceImpl(TeacherHostMapper teacherHostMapper, int offlineSeconds) {
        this.teacherHostMapper = teacherHostMapper;
        this.offlineSeconds = offlineSeconds;
    }

    /**
     * heartbeat的处理逻辑。
     * @param host 主机参数。
     * @return TeacherHostEntity 类型的处理结果。
     */
    @Override
    public TeacherHostEntity heartbeat(TeacherHostEntity host) {
        requirePositiveLabId(host == null ? null : host.getLabId());
        if (isBlank(host.getTeacherDeviceId())) {
            throw new IllegalArgumentException("teacherDeviceId is required");
        }
        if (isBlank(host.getHostIp())) {
            throw new IllegalArgumentException("hostIp is required");
        }
        if (host.getPort() == null || host.getPort() <= 0) {
            throw new IllegalArgumentException("port must be positive");
        }

        host.setStatus("online");
        host.setLastHeartbeatTime(LocalDateTime.now());
        teacherHostMapper.upsertHeartbeat(host);
        return host;
    }

    /**
     * 当前主机的处理逻辑。
     * @param labId 实验室标识参数。
     * @return TeacherHostEntity 类型的处理结果。
     */
    @Override
    public TeacherHostEntity currentHost(Long labId) {
        requirePositiveLabId(labId);
        TeacherHostEntity host = teacherHostMapper.selectCurrentByLabId(labId);
        if (host == null) {
            return null;
        }

        if (!"online".equalsIgnoreCase(host.getStatus()) || isHeartbeatExpired(host.getLastHeartbeatTime())) {
            teacherHostMapper.markOffline(host.getTeacherDeviceId(), host.getLabId());
            host.setStatus("offline");
            return null;
        }

        return host;
    }

    /**
     * 判断是否为HeartbeatExpired。
     * @param heartbeatTime heartbeat时间参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isHeartbeatExpired(LocalDateTime heartbeatTime) {
        if (heartbeatTime == null) {
            return true;
        }
        return Duration.between(heartbeatTime, LocalDateTime.now()).getSeconds() > offlineSeconds;
    }

    /**
     * 要求提供Positive实验室标识。
     * @param labId 实验室标识参数。
     */
    private void requirePositiveLabId(Long labId) {
        if (labId == null || labId <= 0) {
            throw new IllegalArgumentException(LAB_ID_REQUIRED);
        }
    }

    /**
     * 判断是否为空白。
     * @param value 值参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
