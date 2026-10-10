package org.example.backend.service.impl;

import org.example.backend.entity.MaintenanceRecord;
import org.example.backend.mapper.MaintenanceRecordMapper;
import org.example.backend.service.MaintenanceRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
            throw new IllegalArgumentException("maintenance record not found");
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
        ResolvedReferences references = validateAndResolve(record);
        int updated = maintenanceRecordMapper.updateMaintenanceRecord(id, record, references.labId(), references.deviceId());
        if (updated == 0) {
            throw new IllegalArgumentException("maintenance record not found");
        }
        return getMaintenanceRecord(id);
    }

    @Override
    @Transactional
    public void deleteMaintenanceRecord(String id) {
        if (maintenanceRecordMapper.deleteMaintenanceRecord(id) == 0) {
            throw new IllegalArgumentException("maintenance record not found");
        }
    }

    private ResolvedReferences validateAndResolve(MaintenanceRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("maintenance record is required");
        }
        requireValue("lab", record.getLab());
        requireValue("device", record.getDevice());
        requireValue("maintenanceType", record.getMaintenanceType());
        requireValue("maintenanceDate", record.getMaintenanceDate());
        requireValue("maintainer", record.getMaintainer());
        requireValue("content", record.getContent());
        requireValue("result", record.getResult());

        Long labId = maintenanceRecordMapper.findLabId(record.getLab());
        if (labId == null) {
            throw new IllegalArgumentException("lab not found: " + record.getLab());
        }
        Long deviceId = maintenanceRecordMapper.findDeviceId(record.getDevice());
        if (deviceId == null) {
            throw new IllegalArgumentException("device not found: " + record.getDevice());
        }
        return new ResolvedReferences(labId, deviceId);
    }

    private void requireValue(String field, String value) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
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
