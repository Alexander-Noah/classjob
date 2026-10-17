/*
 * 设备管理能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.DevicesEntity;
import org.example.backend.entity.DeviceInventoryRecordEntity;
import org.example.backend.entity.DeviceTransferRecordEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface DevicesService {
    /**
     * 获取设备。
     * @return 当前操作得到的记录集合。
     */
    List<DevicesEntity> getDevices();

    /**
     * 页码设备的处理逻辑。
     * @param query 查询参数。
     * @return DevicePageResult 类型的处理结果。
     */
    DevicePageResult pageDevices(DevicePageQuery query);

    /**
     * 获取设备Stats。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getDeviceStats();

    /**
     * Inserter设备的处理逻辑。
     * @param devicesEntity 设备Entity参数。
     * @return DevicesEntity 类型的处理结果。
     */
    DevicesEntity InserterDevices(DevicesEntity devicesEntity);

    /**
     * 获取设备By标识。
     * @param id 标识参数。
     * @return DevicesEntity 类型的处理结果。
     */
    DevicesEntity getDevicesById(Long id);

    /**
     * 更新设备。
     * @param id 标识参数。
     * @param devices 设备参数。
     * @return DevicesEntity 类型的处理结果。
     */
    DevicesEntity updateDevices(Long id, DevicesEntity devices);

    /**
     * 更新设备。
     * @param devices 设备参数。
     * @return 当前操作得到的记录集合。
     */
    List<DevicesEntity> updateDevices(List<DevicesEntity> devices);

    /**
     * 删除设备。
     * @param id 标识参数。
     */
    void deleteDevices(Long id);

    /**
     * 获取设备By实验室标识。
     * @param labId 实验室标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<DevicesEntity> getDevicesByLabId(Integer labId);

    /**
     * 列出库存记录集合。
     * @param deviceId 设备标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<DeviceInventoryRecordEntity> listInventoryRecords(Long deviceId);

    /**
     * 记录设备库存。
     * @param deviceId 设备标识参数。
     * @param record 记录参数。
     * @return DeviceInventoryRecordResult 类型的处理结果。
     */
    DeviceInventoryRecordResult recordDeviceInventory(Long deviceId, DeviceInventoryRecordEntity record);

    /**
     * 列出Transfer记录集合。
     * @param deviceId 设备标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<DeviceTransferRecordEntity> listTransferRecords(Long deviceId);

    /**
     * 调拨设备。
     * @param deviceId 设备标识参数。
     * @param record 记录参数。
     * @return DeviceTransferRecordResult 类型的处理结果。
     */
    DeviceTransferRecordResult transferDevice(Long deviceId, DeviceTransferRecordEntity record);

    /**
     * 导入设备。
     * @param file 文件参数。
     * @return DeviceImportResult 类型的处理结果。
     */
    DeviceImportResult importDevices(MultipartFile file);

    /**
     * 导出设备。
     * @param query 查询参数。
     * @return byte[] 类型的处理结果。
     */
    byte[] exportDevices(DevicePageQuery query);

    class DevicePageQuery {
        private int pageNum = 1;
        private int pageSize = 10;
        private String keyword;
        private Long labId;
        private String labName;
        private String category;
        private String status;

        /**
         * 获取页码Num。
         * @return int 类型的处理结果。
         */
        public int getPageNum() {
            return pageNum;
        }

        /**
         * 设置页码Num。
         * @param pageNum 页码Num参数。
         */
        public void setPageNum(int pageNum) {
            this.pageNum = pageNum;
        }

        /**
         * 获取页码大小。
         * @return int 类型的处理结果。
         */
        public int getPageSize() {
            return pageSize;
        }

        /**
         * 设置页码大小。
         * @param pageSize 页码大小参数。
         */
        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        /**
         * 获取Keyword。
         * @return String 类型的处理结果。
         */
        public String getKeyword() {
            return keyword;
        }

        /**
         * 设置Keyword。
         * @param keyword keyword参数。
         */
        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        /**
         * 获取实验室标识。
         * @return Long 类型的处理结果。
         */
        public Long getLabId() {
            return labId;
        }

        /**
         * 设置实验室标识。
         * @param labId 实验室标识参数。
         */
        public void setLabId(Long labId) {
            this.labId = labId;
        }

        /**
         * 获取实验室名称。
         * @return String 类型的处理结果。
         */
        public String getLabName() {
            return labName;
        }

        /**
         * 设置实验室名称。
         * @param labName 实验室名称参数。
         */
        public void setLabName(String labName) {
            this.labName = labName;
        }

        /**
         * 获取分类。
         * @return String 类型的处理结果。
         */
        public String getCategory() {
            return category;
        }

        /**
         * 设置分类。
         * @param category 分类参数。
         */
        public void setCategory(String category) {
            this.category = category;
        }

        /**
         * 获取状态。
         * @return String 类型的处理结果。
         */
        public String getStatus() {
            return status;
        }

        /**
         * 设置状态。
         * @param status 状态参数。
         */
        public void setStatus(String status) {
            this.status = status;
        }
    }

    class DevicePageResult {
        private List<DevicesEntity> records;
        private long total;

        /**
         * 构造 DevicePageResult，初始化当前实例。
         * @param records 记录集合参数。
         * @param total 总数参数。
         */
        public DevicePageResult(List<DevicesEntity> records, long total) {
            this.records = records;
            this.total = total;
        }

        /**
         * 获取记录集合。
         * @return 当前操作得到的记录集合。
         */
        public List<DevicesEntity> getRecords() {
            return records;
        }

        /**
         * 设置记录集合。
         * @param records 记录集合参数。
         */
        public void setRecords(List<DevicesEntity> records) {
            this.records = records;
        }

        /**
         * 获取总数。
         * @return long 类型的处理结果。
         */
        public long getTotal() {
            return total;
        }

        /**
         * 设置总数。
         * @param total 总数参数。
         */
        public void setTotal(long total) {
            this.total = total;
        }
    }

    class DeviceImportResult {
        private boolean success;
        private String message;
        private int count;

        /**
         * 构造 DeviceImportResult，初始化当前实例。
         * @param success 成功参数。
         * @param message 消息参数。
         * @param count 数量参数。
         */
        public DeviceImportResult(boolean success, String message, int count) {
            this.success = success;
            this.message = message;
            this.count = count;
        }

        /**
         * 判断是否为成功。
         * @return 当前判断的布尔结果。
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * 设置成功。
         * @param success 成功参数。
         */
        public void setSuccess(boolean success) {
            this.success = success;
        }

        /**
         * 获取消息。
         * @return String 类型的处理结果。
         */
        public String getMessage() {
            return message;
        }

        /**
         * 设置消息。
         * @param message 消息参数。
         */
        public void setMessage(String message) {
            this.message = message;
        }

        /**
         * 获取数量。
         * @return int 类型的处理结果。
         */
        public int getCount() {
            return count;
        }

        /**
         * 设置数量。
         * @param count 数量参数。
         */
        public void setCount(int count) {
            this.count = count;
        }
    }

    class DeviceInventoryRecordResult {
        private DevicesEntity device;
        private DeviceInventoryRecordEntity record;
        private List<DeviceInventoryRecordEntity> records;

        /**
         * 构造 DeviceInventoryRecordResult，初始化当前实例。
         * @param device 设备参数。
         * @param record 记录参数。
         * @param records 记录集合参数。
         */
        public DeviceInventoryRecordResult(DevicesEntity device, DeviceInventoryRecordEntity record, List<DeviceInventoryRecordEntity> records) {
            this.device = device;
            this.record = record;
            this.records = records;
        }

        /**
         * 获取设备。
         * @return DevicesEntity 类型的处理结果。
         */
        public DevicesEntity getDevice() {
            return device;
        }

        /**
         * 设置设备。
         * @param device 设备参数。
         */
        public void setDevice(DevicesEntity device) {
            this.device = device;
        }

        /**
         * 获取记录。
         * @return DeviceInventoryRecordEntity 类型的处理结果。
         */
        public DeviceInventoryRecordEntity getRecord() {
            return record;
        }

        /**
         * 设置记录。
         * @param record 记录参数。
         */
        public void setRecord(DeviceInventoryRecordEntity record) {
            this.record = record;
        }

        /**
         * 获取记录集合。
         * @return 当前操作得到的记录集合。
         */
        public List<DeviceInventoryRecordEntity> getRecords() {
            return records;
        }

        /**
         * 设置记录集合。
         * @param records 记录集合参数。
         */
        public void setRecords(List<DeviceInventoryRecordEntity> records) {
            this.records = records;
        }
    }

    class DeviceTransferRecordResult {
        private DevicesEntity device;
        private DeviceTransferRecordEntity record;
        private List<DeviceTransferRecordEntity> records;

        /**
         * 构造 DeviceTransferRecordResult，初始化当前实例。
         * @param device 设备参数。
         * @param record 记录参数。
         * @param records 记录集合参数。
         */
        public DeviceTransferRecordResult(DevicesEntity device, DeviceTransferRecordEntity record, List<DeviceTransferRecordEntity> records) {
            this.device = device;
            this.record = record;
            this.records = records;
        }

        /**
         * 获取设备。
         * @return DevicesEntity 类型的处理结果。
         */
        public DevicesEntity getDevice() {
            return device;
        }

        /**
         * 设置设备。
         * @param device 设备参数。
         */
        public void setDevice(DevicesEntity device) {
            this.device = device;
        }

        /**
         * 获取记录。
         * @return DeviceTransferRecordEntity 类型的处理结果。
         */
        public DeviceTransferRecordEntity getRecord() {
            return record;
        }

        /**
         * 设置记录。
         * @param record 记录参数。
         */
        public void setRecord(DeviceTransferRecordEntity record) {
            this.record = record;
        }

        /**
         * 获取记录集合。
         * @return 当前操作得到的记录集合。
         */
        public List<DeviceTransferRecordEntity> getRecords() {
            return records;
        }

        /**
         * 设置记录集合。
         * @param records 记录集合参数。
         */
        public void setRecords(List<DeviceTransferRecordEntity> records) {
            this.records = records;
        }
    }
}
