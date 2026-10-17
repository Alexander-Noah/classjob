/*
 * 维护记录能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.MaintenanceRecord;

import java.util.List;

public interface MaintenanceRecordService {
    /**
     * 列出维护记录集合。
     * @return 当前操作得到的记录集合。
     */
    List<MaintenanceRecord> listMaintenanceRecords();

    /**
     * 获取维护记录。
     * @param id 标识参数。
     * @return MaintenanceRecord 类型的处理结果。
     */
    MaintenanceRecord getMaintenanceRecord(String id);

    /**
     * 创建维护记录。
     * @param record 记录参数。
     * @return MaintenanceRecord 类型的处理结果。
     */
    MaintenanceRecord createMaintenanceRecord(MaintenanceRecord record);

    /**
     * 更新维护记录。
     * @param id 标识参数。
     * @param record 记录参数。
     * @return MaintenanceRecord 类型的处理结果。
     */
    MaintenanceRecord updateMaintenanceRecord(String id, MaintenanceRecord record);

    /**
     * 删除维护记录。
     * @param id 标识参数。
     */
    void deleteMaintenanceRecord(String id);
}
