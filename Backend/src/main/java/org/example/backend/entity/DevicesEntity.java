/*
 * 设备管理数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、deviceCode、deviceName、category、labId、labName；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.LocalDateTime;

public class DevicesEntity {
    private Long id;

    @JsonAlias({"assetNo", "asset_no", "code"})
    private String deviceCode;

    @JsonAlias({"deviceName", "device_name", "name"})
    private String deviceName;

    private String category;
    /** 关联实验室的主键；名称/位置用于展示，不能代替关联标识完成写入。 */
    private Long labId;

    @JsonAlias({"labName", "lab_name", "lab"})
    private String labName;

    @JsonAlias({"roomNo", "room_no", "location"})
    private String location;

    private Long ownerUserId;

    @JsonAlias({"owner", "ownerName"})
    private String ownerUsername;

    private Integer quantity;
    private String unit;
    // 仅表示资产生命周期；终端在线及健康状态由 device_runtime_status 维护。
    private String status;
    // 旧 /devices 响应兼容字段，不在资产台账中推断实时健康状态。
    private String health;
    // 旧 /devices 响应兼容字段；真实在线状态应读取终端监控接口。
    private Boolean online;

    @JsonAlias({"specification", "specs"})
    private String specs;

    @JsonAlias({"standardRequirement", "standard_requirement"})
    private String standardRequirement;

    private String remark;
    private String sourceType;
    private String purchaseDate;
    private String inventoryDate;
    private String warrantyDate;
    /** 使用时长，单位小时；接口传递整数，展示层不要按秒再次换算。 */
    private Integer usageHours;
    private String maintenance;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 获取标识。
     * @return Long 类型的处理结果。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置标识。
     * @param id 标识参数。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取设备编码。
     * @return String 类型的处理结果。
     */
    public String getDeviceCode() {
        return deviceCode;
    }

    /**
     * 设置设备编码。
     * @param deviceCode 设备编码参数。
     */
    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    /**
     * 获取资产No。
     * @return String 类型的处理结果。
     */
    public String getAssetNo() {
        return deviceCode;
    }

    /**
     * 设置资产No。
     * @param assetNo 资产No参数。
     */
    public void setAssetNo(String assetNo) {
        this.deviceCode = assetNo;
    }

    /**
     * 获取编码。
     * @return String 类型的处理结果。
     */
    public String getCode() {
        return deviceCode;
    }

    /**
     * 设置编码。
     * @param code 编码参数。
     */
    public void setCode(String code) {
        this.deviceCode = code;
    }

    /**
     * 获取设备名称。
     * @return String 类型的处理结果。
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * 设置设备名称。
     * @param deviceName 设备名称参数。
     */
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    /**
     * 获取名称。
     * @return String 类型的处理结果。
     */
    public String getName() {
        return deviceName;
    }

    /**
     * 设置名称。
     * @param name 名称参数。
     */
    public void setName(String name) {
        this.deviceName = name;
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
     * 获取实验室。
     * @return String 类型的处理结果。
     */
    public String getLab() {
        return labName;
    }

    /**
     * 设置实验室。
     * @param lab 实验室参数。
     */
    public void setLab(String lab) {
        this.labName = lab;
    }

    /**
     * 获取Location。
     * @return String 类型的处理结果。
     */
    public String getLocation() {
        return location;
    }

    /**
     * 设置Location。
     * @param location location参数。
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * 获取RoomNo。
     * @return String 类型的处理结果。
     */
    public String getRoomNo() {
        return location;
    }

    /**
     * 设置RoomNo。
     * @param roomNo roomNo参数。
     */
    public void setRoomNo(String roomNo) {
        this.location = roomNo;
    }

    /**
     * 获取所有者用户标识。
     * @return Long 类型的处理结果。
     */
    public Long getOwnerUserId() {
        return ownerUserId;
    }

    /**
     * 设置所有者用户标识。
     * @param ownerUserId 所有者用户标识参数。
     */
    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    /**
     * 获取所有者Username。
     * @return String 类型的处理结果。
     */
    public String getOwnerUsername() {
        return ownerUsername;
    }

    /**
     * 设置所有者Username。
     * @param ownerUsername 所有者Username参数。
     */
    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    /**
     * 获取Quantity。
     * @return Integer 类型的处理结果。
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * 设置Quantity。
     * @param quantity quantity参数。
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * 获取Unit。
     * @return String 类型的处理结果。
     */
    public String getUnit() {
        return unit;
    }

    /**
     * 设置Unit。
     * @param unit unit参数。
     */
    public void setUnit(String unit) {
        this.unit = unit;
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

    /**
     * 获取Health。
     * @return String 类型的处理结果。
     */
    public String getHealth() {
        return health;
    }

    /**
     * 设置Health。
     * @param health health参数。
     */
    public void setHealth(String health) {
        this.health = health;
    }

    /**
     * 获取Online。
     * @return 当前判断的布尔结果。
     */
    public Boolean getOnline() {
        return online;
    }

    /**
     * 设置Online。
     * @param online online参数。
     */
    public void setOnline(Boolean online) {
        this.online = online;
    }

    /**
     * 获取Specs。
     * @return String 类型的处理结果。
     */
    public String getSpecs() {
        return specs;
    }

    /**
     * 设置Specs。
     * @param specs specs参数。
     */
    public void setSpecs(String specs) {
        this.specs = specs;
    }

    /**
     * 获取Specification。
     * @return String 类型的处理结果。
     */
    public String getSpecification() {
        return specs;
    }

    /**
     * 设置Specification。
     * @param specification specification参数。
     */
    public void setSpecification(String specification) {
        this.specs = specification;
    }

    /**
     * 获取StandardRequirement。
     * @return String 类型的处理结果。
     */
    public String getStandardRequirement() {
        return standardRequirement;
    }

    /**
     * 设置StandardRequirement。
     * @param standardRequirement standardRequirement参数。
     */
    public void setStandardRequirement(String standardRequirement) {
        this.standardRequirement = standardRequirement;
    }

    /**
     * 获取Remark。
     * @return String 类型的处理结果。
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 设置Remark。
     * @param remark remark参数。
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 获取来源类型。
     * @return String 类型的处理结果。
     */
    public String getSourceType() {
        return sourceType;
    }

    /**
     * 设置来源类型。
     * @param sourceType 来源类型参数。
     */
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * 获取Purchase日期。
     * @return String 类型的处理结果。
     */
    public String getPurchaseDate() {
        return purchaseDate;
    }

    /**
     * 设置Purchase日期。
     * @param purchaseDate purchase日期参数。
     */
    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    /**
     * 获取库存日期。
     * @return String 类型的处理结果。
     */
    public String getInventoryDate() {
        return inventoryDate;
    }

    /**
     * 设置库存日期。
     * @param inventoryDate 库存日期参数。
     */
    public void setInventoryDate(String inventoryDate) {
        this.inventoryDate = inventoryDate;
    }

    /**
     * 获取Warranty日期。
     * @return String 类型的处理结果。
     */
    public String getWarrantyDate() {
        return warrantyDate;
    }

    /**
     * 设置Warranty日期。
     * @param warrantyDate warranty日期参数。
     */
    public void setWarrantyDate(String warrantyDate) {
        this.warrantyDate = warrantyDate;
    }

    /**
     * 获取UsageHours。
     * @return Integer 类型的处理结果。
     */
    public Integer getUsageHours() {
        return usageHours;
    }

    /**
     * 设置UsageHours。
     * @param usageHours usageHours参数。
     */
    public void setUsageHours(Integer usageHours) {
        this.usageHours = usageHours;
    }

    /**
     * 获取维护。
     * @return String 类型的处理结果。
     */
    public String getMaintenance() {
        return maintenance;
    }

    /**
     * 设置维护。
     * @param maintenance 维护参数。
     */
    public void setMaintenance(String maintenance) {
        this.maintenance = maintenance;
    }

    /**
     * 获取Deleted。
     * @return Integer 类型的处理结果。
     */
    public Integer getDeleted() {
        return deleted;
    }

    /**
     * 设置Deleted。
     * @param deleted deleted参数。
     */
    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    /**
     * 获取CreatedAt。
     * @return LocalDateTime 类型的处理结果。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置CreatedAt。
     * @param createdAt createdAt参数。
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取UpdatedAt。
     * @return LocalDateTime 类型的处理结果。
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置UpdatedAt。
     * @param updatedAt updatedAt参数。
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
