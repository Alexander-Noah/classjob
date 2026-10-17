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

    public StudentClientServiceImpl(StudentClientMapper studentClientMapper) {
        this.studentClientMapper = studentClientMapper;
    }

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

    @Override
    public void offline(StudentClientEntity client) {
        requirePositiveLabId(client == null ? null : client.getLabId());
        if (isBlank(client.getStudentDeviceId())) {
            throw new IllegalArgumentException("studentDeviceId is required");
        }
        studentClientMapper.markOffline(client.getStudentDeviceId(), client.getLabId());
    }

    @Override
    public List<StudentClientEntity> listByLabId(Long labId) {
        requirePositiveLabId(labId);
        List<StudentClientEntity> clients = studentClientMapper.selectByLabId(labId);
        return clients == null ? List.of() : clients;
    }

    private void requirePositiveLabId(Long labId) {
        if (labId == null || labId <= 0) {
            throw new IllegalArgumentException(TeacherHostServiceImpl.LAB_ID_REQUIRED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
