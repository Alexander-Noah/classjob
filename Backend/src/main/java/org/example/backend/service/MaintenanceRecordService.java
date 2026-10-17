/*
 * 维护记录能力契约，供调用方依赖接口而非具体实现。
 */
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
