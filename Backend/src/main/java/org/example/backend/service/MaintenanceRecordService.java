package org.example.backend.service;

import org.example.backend.entity.MaintenanceRecord;

import java.util.List;

public interface MaintenanceRecordService {
    List<MaintenanceRecord> listMaintenanceRecords();

    MaintenanceRecord getMaintenanceRecord(String id);

    MaintenanceRecord createMaintenanceRecord(MaintenanceRecord record);

    MaintenanceRecord updateMaintenanceRecord(String id, MaintenanceRecord record);

    void deleteMaintenanceRecord(String id);
}
