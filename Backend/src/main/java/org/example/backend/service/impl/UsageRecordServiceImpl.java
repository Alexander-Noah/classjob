/*
 * 使用记录实现模块。
 * 主要协作组件：UsageRecordMapper、BusinessLoopService。
 */
package org.example.backend.service.impl;

import org.example.backend.entity.UsageRecordEntity;
import org.example.backend.mapper.UsageRecordMapper;
import org.example.backend.service.BusinessLoopService;
import org.example.backend.service.UsageRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class UsageRecordServiceImpl implements UsageRecordService {
    private final UsageRecordMapper usageRecordMapper;
    private final BusinessLoopService businessLoopService;

    /**
     * 构造 UsageRecordServiceImpl，初始化当前实例。
     * @param usageRecordMapper usage记录数据映射器参数。
     * @param businessLoopService businessLoop服务参数。
     */
    public UsageRecordServiceImpl(UsageRecordMapper usageRecordMapper, BusinessLoopService businessLoopService) {
        this.usageRecordMapper = usageRecordMapper;
        this.businessLoopService = businessLoopService;
    }

    /**
     * 列出Usage记录集合。
     * @return 当前操作得到的记录集合。
     */
    @Override
    public List<UsageRecordEntity> listUsageRecords() {
        List<UsageRecordEntity> records = usageRecordMapper.listUsageRecords();
        return records == null ? List.of() : records;
    }

    /**
     * replaceUsage记录集合的处理逻辑。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param records 记录集合参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    @Transactional
    public List<UsageRecordEntity> replaceUsageRecords(List<UsageRecordEntity> records) {
        if (records == null || records.isEmpty()) {
            return listUsageRecords();
        }
        usageRecordMapper.deleteAllUsageRecords();
        for (UsageRecordEntity record : records) {
            normalizeRecord(record);
            usageRecordMapper.insertUsageRecord(record);
            if (record.getStatus() != null && record.getStatus().contains("异常")) {
                businessLoopService.createRepairAfterUsageAbnormal(record);
            }
        }
        businessLoopService.recordEvent("usage-record", "batch-save", "使用记录台账", "已同步", Map.of("count", records.size()));
        return listUsageRecords();
    }

    /**
     * 重置Usage记录集合。
     * @return 当前操作得到的记录集合。
     */
    @Override
    public List<UsageRecordEntity> resetUsageRecords() {
        List<UsageRecordEntity> records = listUsageRecords();
        businessLoopService.recordEvent("usage-record", "reset", "使用记录台账", "已恢复当前台账", Map.of("count", records.size()));
        return records;
    }

    /**
     * 更新状态。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param id 标识参数。
     * @param status 状态参数。
     * @return UsageRecordEntity 类型的处理结果。
     */
    @Override
    @Transactional
    public UsageRecordEntity updateStatus(Long id, String status) {
        int updatedCount = usageRecordMapper.updateUsageRecordStatus(id, status);
        if (updatedCount == 0) {
            throw new IllegalArgumentException("未找到使用记录");
        }
        UsageRecordEntity usageRecord = usageRecordMapper.getUsageRecord(id);
        if (status != null && status.contains("异常")) {
            businessLoopService.createRepairAfterUsageAbnormal(usageRecord);
        } else {
            businessLoopService.recordEvent("usage-record", "status", usageRecord.getResource(), status, Map.of(
                    "usageRecordId", id,
                    "person", usageRecord.getPerson()
            ));
        }
        return usageRecord;
    }

    /**
     * 归一化记录。
     * @param record 记录参数。
     */
    private void normalizeRecord(UsageRecordEntity record) {
        if (record.getUseTime() == null) {
            record.setUseTime(LocalDateTime.now());
        }
        if (record.getStatus() == null || record.getStatus().isBlank()) {
            record.setStatus("正常");
        }
    }
}
