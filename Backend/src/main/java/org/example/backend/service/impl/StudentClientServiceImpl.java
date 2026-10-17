package org.example.backend.service.impl;

import org.example.backend.entity.StudentClientEntity;
import org.example.backend.mapper.StudentClientMapper;
import org.example.backend.service.StudentClientService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** 以实验室和学生设备 ID 登记上下线；调用方必须携带有效 labId，避免设备串入其他实验室。 */
@Service
public class StudentClientServiceImpl implements StudentClientService {
    private final StudentClientMapper studentClientMapper;

    /**
     * 构造 StudentClientServiceImpl，初始化当前实例。
     * @param studentClientMapper 学生客户端数据映射器参数。
     */
    public StudentClientServiceImpl(StudentClientMapper studentClientMapper) {
        this.studentClientMapper = studentClientMapper;
    }

    /**
     * online的处理逻辑。
     * @param client 客户端参数。
     * @return StudentClientEntity 类型的处理结果。
     */
    @Override
    public StudentClientEntity online(StudentClientEntity client) {
        requirePositiveLabId(client == null ? null : client.getLabId());
        if (isBlank(client.getStudentDeviceId())) {
            throw new IllegalArgumentException("studentDeviceId is required");
        }

        client.setStatus("online");
        client.setLastHeartbeatTime(LocalDateTime.now());
        studentClientMapper.upsertOnline(client);
        return client;
    }

    /**
     * offline的处理逻辑。
     * @param client 客户端参数。
     */
    @Override
    public void offline(StudentClientEntity client) {
        requirePositiveLabId(client == null ? null : client.getLabId());
        if (isBlank(client.getStudentDeviceId())) {
            throw new IllegalArgumentException("studentDeviceId is required");
        }
        studentClientMapper.markOffline(client.getStudentDeviceId(), client.getLabId());
    }

    /**
     * 列出By实验室标识。
     * 涉及：读取数据库。
     * @param labId 实验室标识参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    public List<StudentClientEntity> listByLabId(Long labId) {
        requirePositiveLabId(labId);
        List<StudentClientEntity> clients = studentClientMapper.selectByLabId(labId);
        return clients == null ? List.of() : clients;
    }

    /**
     * 要求提供Positive实验室标识。
     * @param labId 实验室标识参数。
     */
    private void requirePositiveLabId(Long labId) {
        if (labId == null || labId <= 0) {
            throw new IllegalArgumentException(TeacherHostServiceImpl.LAB_ID_REQUIRED);
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
