package org.example.backend.service.impl;

import org.example.backend.entity.MaintenanceRecord;
import org.example.backend.mapper.MaintenanceRecordMapper;
import org.example.backend.service.MaintenanceRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MaintenanceRecordServiceImpl implements MaintenanceRecordService {
    private static final DateTimeFormatter RECORD_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MaintenanceRecordMapper maintenanceRecordMapper;

    public MaintenanceRecordServiceImpl(MaintenanceRecordMapper maintenanceRecordMapper) {
        this.maintenanceRecordMapper = maintenanceRecordMapper;
    }

    @Override
    public List<MaintenanceRecord> listMaintenanceRecords() {
        List<MaintenanceRecord> records = maintenanceRecordMapper.listMaintenanceRecords();
        return records == null ? List.of() : records;
    }

    @Override
    public MaintenanceRecord getMaintenanceRecord(String id) {
        MaintenanceRecord record = maintenanceRecordMapper.getMaintenanceRecord(id);
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "维护记录不存在或已删除");
        }
        return record;
    }

    @Override
    @Transactional
    public MaintenanceRecord createMaintenanceRecord(MaintenanceRecord record) {
        ResolvedReferences references = validateAndResolve(record);
        record.setRecordNo(newRecordNo());
        maintenanceRecordMapper.insertMaintenanceRecord(record, references.labId(), references.deviceId());
        return getMaintenanceRecord(record.getRecordNo());
    }

    @Override
    @Transactional
    public MaintenanceRecord updateMaintenanceRecord(String id, MaintenanceRecord record) {
        MaintenanceRecord existing = getMaintenanceRecord(id);
        ResolvedReferences references = validateAndResolve(record);
        int updated = maintenanceRecordMapper.updateMaintenanceRecord(existing.getId(), record, references.labId(), references.deviceId());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "维护记录不存在或已删除");
        }
        return getMaintenanceRecord(existing.getId());
    }

    @Override
    @Transactional
    public void deleteMaintenanceRecord(String id) {
        MaintenanceRecord existing = getMaintenanceRecord(id);
        if (maintenanceRecordMapper.deleteMaintenanceRecord(existing.getId()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "维护记录不存在或已删除");
        }
    }

    private ResolvedReferences validateAndResolve(MaintenanceRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("请填写维护记录");
        }
        if (record.getLabId() == null) requireValue("lab", record.getLab());
        requireValue("maintenanceType", record.getMaintenanceType());
        requireValue("maintenanceDate", record.getMaintenanceDate());
        requireValue("maintainer", record.getMaintainer());
        requireValue("content", record.getContent());
        requireValue("result", record.getResult());

        limit("维护类型", record.getMaintenanceType(), 100);
        limit("维护人员", record.getMaintainer(), 100);
        limit("维护内容", record.getContent(), 5000);
        limit("处理结果", record.getResult(), 5000);
        limit("备注", record.getRemark(), 100);
        record.setMaintenanceDate(record.getMaintenanceDate().trim());
        try {
            String date = record.getMaintenanceDate().trim();
            if (date.length() == 10) LocalDate.parse(date);
            else LocalDateTime.parse(date.replace(' ', 'T'));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("请选择有效的维护日期");
        }
        Long labId = maintenanceRecordMapper.findLabId(record.getLabId() == null ? record.getLab().trim() : record.getLabId().toString());
        if (labId == null || (record.getLabId() != null && !record.getLabId().equals(labId))) {
            throw new IllegalArgumentException("所选实验室不存在，请重新选择");
        }
        if (record.getDeviceId() == null && (isBlank(record.getDevice()) || "未填写".equals(record.getDevice().trim()))) {
            return new ResolvedReferences(labId, null);
        }
        Long deviceId = maintenanceRecordMapper.findDeviceId(record.getDeviceId() == null ? record.getDevice().trim() : record.getDeviceId().toString(), labId);
        if (deviceId == null || (record.getDeviceId() != null && !record.getDeviceId().equals(deviceId))) {
            throw new IllegalArgumentException("所选设备不存在或不属于该实验室，请重新选择");
        }
        return new ResolvedReferences(labId, deviceId);
    }

    private void requireValue(String field, String value) {
        if (isBlank(value)) {
            String label = switch (field) {
                case "lab" -> "实验室"; case "device" -> "设备";
                case "maintenanceType" -> "维护类型"; case "maintenanceDate" -> "维护日期";
                case "maintainer" -> "维护人员"; case "content" -> "维护内容";
                case "result" -> "处理结果"; default -> field;
            };
            throw new IllegalArgumentException("请填写" + label);
        }
    }

    private void limit(String field, String value, int max) {
        if (value != null && value.length() > max) throw new IllegalArgumentException(field + "不能超过" + max + "字");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String newRecordNo() {
        return "MR-" + LocalDateTime.now().format(RECORD_NO_TIME)
                + "-" + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private record ResolvedReferences(Long labId, Long deviceId) {
    }
}
